#Justext Demo Application#

This is a Java Swing aplication to demonstrate the Justext library.

The all-in-one jar file contains all the neccessary dependencies ready to run on **Windows OS x64**. 

For other platform (Win32, Linux..) build the package yourself. (Sorry!)

##Requirement

- Windows x64
- Java 1.6+
- Authorization to write log files to the working directory

##How to run

From the command line:

    java -jar JusTextDemo.jar

or

    java -cp JusTextDemo.jar dh.tool.justext.demo.MainApp

##User Guide

- Enter address of any article and click `GO` to extract the main content
- You might override the default configuration of the extractor to test. Click `Reset` to see the default configuration, and tweak from this. You have 3 configuration editor to try different configuration set
- The extraction result is displayed in several diffrent WebView 
    + **Final** - result from extractor (page after removing boilerplates)
    + **Decoration** - remove basic useless content (pre-process) then decorate higlight boilerplate in red, main content in green
    + **Original** - article original
    + **Pre-porcess** - remove basic useless content (`script`, `form`, `style`...)
    + **Auto-detect Language** - result from extractor if enable the language-awareness (the extractor is more strict)
    + **Auto-detect Language Decoration** - result hilighting from extractor after enabling language-awareness (the extractor is more strict)

- In each WebView (Final, Decoration..), you can View Page Source, and search inside it with
    + **Ctrl+F** - Open search bar
    + **Enter** - find next
    + **Shift+Enter** - find previous

- At the bottom of WebView is the status of the extraction process
    + Time spent in extraction process (not the downloading process)
    + Address

- Each component in the interface is an individual module which communicate with each other via Event Bus, I did not have time to carefully program the synchronisation between components. So you might meet some odd behaviour, but it is not bug. For example: if you click `GO` 2 times to process 2 diffrent articles, you will see the result of the latest Article which finish the processing, it might NOT the last article that you clicked on `GO`.

- Justext is language-independant by default, to enable language-awareness you can give the following config to the extractor
    +  `language = fr;` or `language = en;` so you tell the extractor which is the language of the article.
    +  or `autoDetectLanguage = true;` So it will try to detect the language of the article
    +  If the extractor does not possess the corresponding stopwords dictionary, it will automaticly disable the language-awareness during extraction process. The language-awareness extractor is more strict, it rejects paragraphs which are grammatically meaningless.

##Notice

- This application helps you to quickly test the my Justext library with minimum requirement (no need to `git`, no need to `build`..). But It is not warranty to be use the latest version of Justext.
- This application is heavy (~7Mb) because I take occasion to experiment diffrent swing libraries which might be over-necessary for such a small application.
- But the Justext library is small and it is designed to use in Android. Justext only depends on [Jsoup](http://jsoup.org/) and [Guava v15](https://code.google.com/p/guava-libraries/)
- I dev and watching WorldCup 2014 at the sametime, you might found many bug and odd design decision / architect everywhere. Please don't take this as good application design.