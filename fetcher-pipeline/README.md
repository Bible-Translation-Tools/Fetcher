# fetcher-pipeline

### Requirements

Python 3.8  

Java 11  

NodeJs 14  

### How to use  

#### worker.sh script:  

`./worker.sh -i /path/to/input/directory -hr 1 -mn 0 -t -v`  

**options:**  

`-i (--input-dir)` - Input directory (Normally ftp root dir)  
`-hr (--hour)` - Frequency of executing workers in hours (default: 1). Every hour  
`-mn (--minute)` - Frequency of executing workers in minutes (default: 0)  
`-t (--trace)` - (Optional) Enable tracing output (default: False)  
`-v (--verbose)` - (Optional) Enable logs from subprocess (default: False)  

An example to write log to a file:  

`./worker.sh -t -v -i /path/to/input/directory 2>> path/to/file.log`

Alternatively you can run python script separately  

#### python script:  

**App:**

`python app.py -i /path/to/input/directory -hr 1 -mn 0 -t -v`

**options:**  

`-i (--input-dir)` - Input directory (Normally ftp root dir)  
`-hr (--hour)` - Frequency of executing workers in hours (default: 1). Every hour  
`-mn (--minute)` - Frequency of executing workers in minutes (default: 0)  
`-t (--trace)` - (Optional) Enable tracing output  
`-v (--verbose)` - (Optional) Enable logs from subprocess  

**App workers description**

**Chapter worker**

Finds chapter wav files, splits them into verses and converts all to mp3 files  

**Verse Worker:**

Finds verse wav files and converts them into mp3 files  

**TR Worker:**

Finds verse wav and mp3 files, groups them into books and chapters and creates TR files  
