%.class: %.java 
	javac -classpath junit/*:checkstyle/*:. $<
docs:
	./test.sh
clean:
	rm -r *~ *.class *.out 
