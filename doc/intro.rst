.. _intro:

Introduction
============

What is Qualimap?
-----------------

Qualimap is a platform-independent application written in Java and R that provides both a Graphical User Interface (GUI) and a command-line interface to facilitate the quality control of alignment sequecing data. The aim of Qualimap is to provide an easy way for the quality control of mapping data, considering the sequence features and their genomic properties.

Basically, the application accepts and examines sequence alignment data, summarizing some interesting issues found in such data. The main features offered by Qualimap are: fast analysis across the reference genome of mapping coverage and nucleotide relative content; easy-to-interpret summary of the main properties of the alignment data; analysis of the reads mapped inside/outside of the regions defined in an annotation reference; analysis of the adequacy of the sequencing depth.

Installation
------------

Download the ZIP archive with Qualimap from the `Qualimap web page <http://qualimap.org>`_.

Unpack it to desired directory. 

Now you can run Qualimap from this directory using the prebuilt script:

:samp:`./qualimap`

Qualimap was tested on GNU Linux, MacOS and MS Windows.

.. note:: On MS Windows use script qualimap.bat to launch Qualimap.

Requirements
------------

Qualimap requires
 
* JAVA runtime (6 or above)
* R enviroment (2.14 or above)

The JAVA runtime can be downloaded from the `official web-site <http://www.java.com>`_.
There are prebuilt binaries available for many platforms.

R enviroment can be downloaded from `R project web-site <http://www.r-project.org>`_. 

Several Qualimap features are implemented in R, using a number of external packages.

.. note:: If R enviroment is not availble, "Epigenetics" and "RNA-seq" features will be disabled.

Currently Qualimap depends on the following R-packages:
 
* optparse (available from `CRAN <http://cran.r-project.org>`_)
* Repitools, Rsamtools, GenomicFeatures, rtracklayer (available from `Bioconductor <http://bioconductor.org>`_) 

One can install these packages manually or use the script from Qualimap distribution.

Once R envronment is avilable the installation script can be invoked from Qualimap folder:

:samp:`Rscript scripts/installDependencies.r`

.. note:: In general installation of Qualimap dependencies is platform-specific and may required additinal effort.

Installing Qualimap on Ubuntu
---------------------------------------

This manual is specific for Ubuntu(Debian) Linux distributive, however with slight differences this can be applied for others Unix systems. 

Install JAVA
^^^^^^^^^^^^

It is possible to use openjdk:

:samp:`sudo apt-get install openjdk-6-jre`

Install R
^^^^^^^^^

The R latest version can be installede from public repos.

However, the repos must be added to the sources. Open sources.list:

:samp:`sudo gedit /etc/apt/sources.list`

Add the following lines:

:samp:`deb http://<my.favorite.cran.mirror>/bin/linux/ubuntu <name.of.your.distribution>/`
 
Then install R:

:samp:`sudo apt-get update`  

:samp:`sudo apt-get install r-base-core`
 
If you don't have the public key for the mirror add it:

:samp:`gpg --keyserver subkeys.pgp.net --recv-key <required.key>`

:samp:`gpg -a --export <required.key> | sudo apt-key add -`

More details available here:
 
   https://stat.ethz.ch/pipermail/r-help/2009-February/187644.html

   http://cran.r-project.org/bin/linux/ubuntu/README

.. note:: Alternatively it is possible to build R enviroment directly from sources downloaded from r-project.org.

Install required R-packages
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Use special script from Qualimap pacage:

:samp:`Rscript $QUALIMAP_HOME/scripts/installDependencies.r`

where :samp:`QUALIMAP_HOME` contains full path to the Qualimap folder.



