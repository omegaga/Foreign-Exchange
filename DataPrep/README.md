# Data Preparation
## Organization:
This program consists of the following files:

* `runner.py`: runs the script to process files
* `processor.py`: defines and implements class of processing files
* `sliding_window.py`: implements sliding window
* `config.py`: configuration file

## Usage:
`python runner.py <input_path>`

## Configuration
* `ESTIMATE_CURPAIR`: Currency pair to estimate
* `OUTPUT_FILE`: The file to store output
* `TIMESLOT_LEN`: length of a time slot in minutes (See LABELING\_METHOD)
* `WINDOW_SIZE`: The size of sliding window (See LABELING\_METHOD)
