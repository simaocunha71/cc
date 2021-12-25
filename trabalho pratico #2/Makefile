IN=testeIn
OUT=testeOut
LOGS=Logs
HTTP=http_requests

CC=javac

all:
	$(CC) Fast_Sync.java DirController.java DirWatcher.java FileState.java  FileStat.java Reciver.java Sender.java Logs.java

checkTransfer:
	diff testeIn testeOut

cleanOut:
	rm -f $(OUT)/*
cleanIn:
	rm -f $(IN)/*
cleanLogs:
	rm -f $(LOGS)/*
cleanClass:
	rm -f $(HTTP)/*.class 
	rm -f *.class

clean : cleanOut cleanIn cleanLogs cleanClass