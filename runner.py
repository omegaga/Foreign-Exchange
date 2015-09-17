""" Description:
        Runner to process data
    Author: Jianhong Li
    Andrew ID: jianhonl
"""

from processor import Processor
import config
import sys


def main():
    """ Main function """
    p = Processor()
    p.process(sys.argv[1],
              output=config.OUTPUT_FILE,
              estimate_curpair=config.ESTIMATE_CURPAIR,
              window_size=config.WINDOW_SIZE,
              timeslot_len=config.TIMESLOT_LEN)


if __name__ == "__main__":
    main()
