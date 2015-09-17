from itertools import islice
""" Description:
        Implement a sliding window based on itertools
    Author: Jianhong Li
    Andrew ID: jianhonl
"""


def window(seq, n):
    """ Returns a sliding window of length n """
    it = iter(seq)
    result = tuple(islice(it, n))

    if len(result) == n:
        yield result
    for item in it:
        result = result[1:] + (item,)
        yield result
