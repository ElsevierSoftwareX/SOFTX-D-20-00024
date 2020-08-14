# Peptide Mind

PeptideMind is a Kotlin-Python hybrid program that incorporates machine learning processes to perform statistical, orthoganol variation of differentially regulated protein identifiers from a shotgun proteomics experiment.

# Installation

Python requirements can be found in the requirements.txt, and the kotlin front-end processes requires an installation from gradle.
A future upgrade will switch over to poetry for the python environment.

# Usage

PeptideMind is designed with an intuitive user interface to guide the user to proper analysis. 

1. Users upload a folder containing 6 control replicates and a separate folder containing 6 treatment replicates.
2. An output folder destination is selected by the user
3. USers select which ML algorithms to use
4. The input file PSM search engine style (GPM, MM, PD) is chosen to match user data types.
5. Analysis starts once the user clickls run!