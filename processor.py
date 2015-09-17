#!/usr/bin/env python
""" Description:
        Data processor to generate features
    Author: Jianhong Li
    Andrew ID: jianhonl
"""
from zipfile import ZipFile
from datetime import datetime, timedelta
from copy import deepcopy
from sliding_window import window
import os
import re


TIMESLOT_LEN = timedelta(hours=1)


class Processor:
    """ Processor for currency data to prepare data for classification """

    def __unarchive_zipfile(self, filename):
        """ Reads CSV file inside the zip file and returns file object """
        file_obj = open(filename, 'rb')
        zip_file = ZipFile(file_obj)
        # there should be only one file in zip archive
        csv_filename = zip_file.namelist()[0]
        return zip_file.open(csv_filename)

    def __parse_line(self, line_str):
        """ Parses a line in CSV file """
        row = line_str.strip().split(',')
        row[1] = datetime.strptime(row[1], '%Y%m%d %H:%M:%S.%f')
        row[2] = float(row[2])
        row[3] = float(row[3])
        return row

    def __get_currency_pair(self, filename):
        """ Reads the currency pair from the file name """
        r = re.search('[A-Z]{6}', filename)
        return r.group()

    def __aggregate_data(self, file_obj, curpair, data):
        """ Read a CSV file and aggregate data in timeslot view """
        first = True
        current_data = []
        for line_str in file_obj.readline():
            row = self.__parse_line(line_str)
            if first:
                # Initialize start time
                start_time = datetime(row[1].year, row[1].month, 1)
                current_timebound = start_time + TIMESLOT_LEN
                first = False
                continue

            time = row[1]
            if time < current_timebound:
                current_data.append(row)
            else:
                # Aggregate data into timeslot view
                if current_timebound not in data:
                    data[current_timebound] = {}
                if len(current_data) == 0:
                    # Smooth data if no data found in current timeslot
                    if len(data) > 0:
                        data[current_timebound][curpair] = {}
                        last_timebound = current_timebound - TIMESLOT_LEN
                        if last_timebound in data:
                            data[current_timebound][curpair] =\
                                deepcopy(data[last_timebound][curpair])
                else:
                    # Record max, min, open, close price
                    data[current_timebound][curpair] = {
                        'bid_max': max([e[2] for e in current_data]),
                        'bid_min': min([e[2] for e in current_data]),
                        'bid_opn': current_data[0][2],
                        'bid_cls': current_data[-1][2],
                        'ask_max': max([e[3] for e in current_data]),
                        'ask_min': min([e[3] for e in current_data]),
                        'ask_opn': current_data[0][3],
                        'ask_cls': current_data[-1][3]}
                current_timebound += TIMESLOT_LEN
                current_data = []

    def __print_result(self, fp, seq, est_curpair):
        """ Print data in terms of features and label """
        label_row = seq[-1]
        prev_row = seq[-2]
        feature_rows = seq[:-1]

        # extract feature from a timeslot
        features = [
            price
            for row in feature_rows
            for curpair in row.values()
            for price in curpair.values()]

        # get directionality of current timeslot as label
        direction = int(label_row[est_curpair]['bid_cls'] >
                        prev_row[est_curpair]['bid_cls'])

        # print processed data to output file
        for feature in features:
            fp.write('%f ' % feature)
        fp.write('%d\n' % direction)

    def process(self, root_dir, **kwargs):
        """ TODO """
        # Read keyword arguments
        est_curpair = kwargs.get('estimate_curpair', 'EURUSD')
        output_file = kwargs.get('output', 'output')
        window_size = kwargs.get('window_size', 3)

        # Extract zip files from directory
        zipfiles = [
            [
                os.path.join(root_dir, curpair, zipfile)
                for zipfile in os.listdir(os.path.join(root_dir, curpair))
            ] for curpair in os.listdir(root_dir)]

        with open(output_file, 'w') as fp:
            months = len(zipfiles[0])  # number of months in given dataset
            for month in xrange(months):
                # get zip files of current month
                month_zipfiles = [e[month] for e in zipfiles]

                # initialize data
                data = {}

                for filename in month_zipfiles:
                    with self.__unarchive_zipfile(filename) as file_obj:
                        self.__aggregate_data(
                            file_obj,
                            self.__get_currency_pair(filename),
                            data)

                # Iterate data with a sliding window, for each sequence, the
                # first n items serve as feature and last item serves as label
                data_it = iter(data[i] for i in sorted(data))
                for seq in window(data_it, window_size + 1):
                    self.__print_result(fp, seq, est_curpair)
