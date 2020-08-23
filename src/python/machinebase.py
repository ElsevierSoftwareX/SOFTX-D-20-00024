from functools import reduce
import pandas as pd
import glob
import numpy as np
import matplotlib.pyplot as plt
from sklearn.ensemble import IsolationForest
from sklearn.ensemble import RandomForestClassifier
from sklearn.ensemble import ExtraTreesClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.tree import DecisionTreeClassifier
import datetime as dt
import os
import sys


def cleanData(imports):

    outerjoin = reduce(lambda a, b: pd.merge(a, b, on='ID', how='outer'), imports)
    outerjoin.set_index('ID', drop=True, inplace=True)
    outerjoin.columns = [str(i+1) for i in range(0, len(outerjoin.columns))]
    outerjoin = outerjoin.loc[:, '1':'400'].apply(pd.to_numeric, errors='coerce')
    outerjoin.fillna(1, inplace=True)
    outerjoin.replace(0, 1, inplace=True)

    return outerjoin


def importCSV(kfilepath, tfilepath, catfilepath):

    print("Importing CSV files...")

    ids = glob.glob(kfilepath + "*.csv")
    outerjoinln = cleanData([pd.read_csv(file, delimiter=',', header=None, names=['ID', 'ttest'], usecols=[0,1],
                                         quotechar='"') for file in ids])
    outerjoinsp = cleanData([pd.read_csv(file, delimiter=',', header=None, names=['ID', 'ttest'], usecols=[0,2],
                                         quotechar='"') for file in ids])

    test = pd.read_csv(tfilepath, delimiter=',', header=None, names=['ID', 'lnnsaf', 'spc'], quotechar='"')
    test.set_index('ID', drop=True, inplace=True)

    label = pd.read_csv(catfilepath, delimiter=',', header=None)

    return outerjoinln, outerjoinsp, test, label


def individualForests(kerberusln, kerberussp, testdata, type, time, look, outputfolder):

    print("Producing " + type + " forests")

    count = 0
    predictiontest = []

    if type == "Isolation":
        forest = IsolationForest(max_samples='auto', contamination=0.2)
        os.makedirs(outputfolder + 'Individual/IsolationForest/')
        foldername = outputfolder + 'Individual/IsolationForest/'

    if type == "RandomClassifier":
        forest = RandomForestClassifier(n_estimators=100)
        os.makedirs(outputfolder + 'Individual/RandomClassifierForest/')
        foldername = outputfolder + 'Individual/RandomClassifierForest/'

    # This section here will produce 400 separate forests, 1 for every ttest combo,
    # indexed to a protein ID

    for index, row in testdata.iterrows():  # index = ID and row = all 400 values
        count += 1

        if look != ["All"]:
            if index not in look:
                continue

        if index not in kerberusln.index.values:
            print("Test not in 400Data...? Why?")
            continue

        foresttrain = pd.concat([kerberusln.loc[index], kerberussp.loc[index]], axis=1)
        foresttrain.columns = ["lnNSAF", "SpC"]
        foresttrain.fillna(1, inplace=True)
        foresttrain.replace(' NaN', 1, inplace=True)
        foresttrain.drop(foresttrain[(foresttrain.lnNSAF == 1) | (foresttrain.SpC == 1) |
                                     (foresttrain.lnNSAF == 0) | (foresttrain.SpC == 0)].index, inplace=True)

        test = pd.DataFrame(testdata.loc[index].values.reshape(1, -1))
        test.columns = ["lnNSAF", "SpC"]
        test.fillna(1, inplace=True)
        test.replace(' NaN', 1, inplace=True)
        test.drop(test[(test.lnNSAF == 1) | (test.SpC == 1) | (test.lnNSAF == 0) | (test.SpC == 0)].index, inplace=True)

        try:
            print("Tree " + str(count) + " of " + str(len(testdata.index)) + " completed.")
            if type == "Isolation":
                forest.fit(foresttrain)
                predictiontest.append(forest.predict(test))  # This tells us whether the test is an outlier/in
            if type == "RandomClassifier":
                forest.fit(foresttrain, foresttrain)
                predictiontest.append(forest.predict(test))
        except ValueError:
            print("Warning: Incompatible Test @: " + str(count))
            #test.to_csv(str("src/output/error/" + str(count) + "-Test.csv"), sep=',')
            #foresttrain.to_csv(str("src/output/error/" + str(count) + "-Train.csv"), sep=',')
            continue

        plt.clf()
        index = index.replace("|", "-")
        plt.title(index + " " + type)
        xx, yy = np.meshgrid(np.linspace(0, 1, 50), np.linspace(0, 1, 50))
        z = forest.decision_function(np.c_[xx.ravel(), yy.ravel()])
        z = z.reshape(xx.shape)
        plt.contourf(xx, yy, z, cmap=plt.cm.Greens_r)
        b1 = plt.scatter(foresttrain["lnNSAF"], foresttrain["SpC"], c='black', s=1, edgecolor='k')
        b2 = plt.scatter(test["lnNSAF"], test["SpC"], c='yellow', s=20)
        plt.legend([b1, b2], ["training observations", "new observations"], loc="upper right")
        plt.xlabel("TTest from lnNSAF")
        plt.ylabel("TTest from SpC")
        plt.yticks(np.arange(0, 1, step=0.05))
        plt.xticks(np.arange(0, 1, step=0.05), rotation=75)
        plt.tight_layout()
        plt.savefig(foldername + index + '.jpg')
        plt.close("all")


def constructDataFrames(kerberusln, kerberussp, testdata):

    print("Making the Train and Test Dataframes for the Mega Forest...")

    totaldf = pd.concat([pd.Series(kerberusln.values.ravel()), pd.Series(kerberussp.values.ravel())], axis=1)
    totaldf.columns = ["lnNSAF", "SpC"]
    totaldf.replace(' NaN', 1, inplace=True)
    totaldf.replace('NaN', 1, inplace=True)
    totaldf.fillna(1, inplace=True)
    totaldf.drop(totaldf[(totaldf.lnNSAF == 1) | (totaldf.SpC == 1) |
                         (totaldf.lnNSAF == 0) | (totaldf.SpC == 0) |
                         (totaldf.lnNSAF.dtype == np.str) | (totaldf.SpC.dtype == np.str)].index, inplace=True)
    totaldf.reset_index(drop=True, inplace=True)

    testdf = testdata
    testdf.columns = ["lnNSAF", "SpC"]
    testdf.replace(' NaN', 1, inplace=True)
    testdf.replace('NaN', 1, inplace=True)
    testdf.fillna(1, inplace=True)
    testdf.drop(testdf[(testdf.lnNSAF == 1) | (testdf.SpC == 1) |
                       (testdf.lnNSAF == 0) | (testdf.SpC == 0) |
                       (testdf.lnNSAF.dtype == np.str) | (testdf.SpC.dtype == np.str)].index, inplace=True)
    testdf.reset_index(drop=True, inplace=True)

    return totaldf, testdf


def megaIsolationForest(totaldf, testdf):

    forest = IsolationForest(max_samples='auto')

    # This section computes a mega-forest.

    print("Now constructing the mega forest")
    forest.fit(totaldf)
    predictiontest = forest.predict(testdf)

    plt.clf()
    plt.title("Isolation Forest")
    xx, yy = np.meshgrid(np.linspace(0, 1, 50), np.linspace(0, 1, 50))
    z = forest.decision_function(np.c_[xx.ravel(), yy.ravel()])
    z = z.reshape(xx.shape)
    plt.contourf(xx, yy, z, cmap=plt.cm.Greens_r)
    colours = ['yellow' if test == 1 else 'red' for test in predictiontest]
    b1 = plt.scatter(totaldf["lnNSAF"], totaldf["SpC"], c='black', s=.1)
    b2 = plt.scatter(testdf["lnNSAF"], testdf["SpC"], c=colours, s=30, edgecolor='k')
    plt.legend([b1, b2], ["Training", "Test Inlier"], loc="center left", bbox_to_anchor=(1.05, 1))
    plt.plot((0.05, 0.05), (0, 1), c='blue')  # (x1 x2), (y1 y2)
    plt.plot((0, 1), (0.05, 0.05), c='blue')
    plt.xlabel("TTest from lnNSAF")
    plt.ylabel("TTest from SpC")
    plt.yticks(np.arange(0, 1, step=0.05))
    plt.xticks(np.arange(0, 1, step=0.05), rotation=75)
    plt.show()
    plt.close("all")


def multiLabelMultiClassifier(kerberusln, kerberussp, testdata, labels, usertypes, outputfolder):

    os.mkdir(outputfolder + "/MultiLabel/")

    forestcollection = []
    possibletypes = ["KNeighbors", "Random Forest", "Decision Tree", "Extra Trees"]
    forestinit = [
        KNeighborsClassifier(n_jobs=-1),
        RandomForestClassifier(testdata.shape[0], n_jobs=-1),
        DecisionTreeClassifier(),
        ExtraTreesClassifier(testdata.shape[0], n_jobs=-1)]
    widths = [0.8, 0.65, 0.5, 0.35]
    colours = ['#0a3052', '#161fc0', '#486bea', '#7fbbf0']

    # This is a second round of data cleaning
    traindatalist = []
    testdatalist = []
    for index, row in testdata.iterrows():  # index = ID and row = all 400 values

        if index not in kerberusln.index.values:
            print("Test not in 400Data...? Why?")
            continue

        foresttrain = pd.concat([kerberusln.loc[index], kerberussp.loc[index]], axis=1)
        for i in range(0, 12):
            foresttrain["Label " + str(i + 1)] = labels.iloc[:, i].astype(str).values
        foresttrain.columns = ["lnNSAF", "SpC", *list("Label " + str(i) for i in range(1, 13))]
        foresttrain.fillna(1, inplace=True)
        foresttrain.replace(' NaN', 1, inplace=True)
        foresttrain.drop(foresttrain[(foresttrain.lnNSAF == 1) | (foresttrain.SpC == 1) |
                                     (foresttrain.lnNSAF == 0) | (foresttrain.SpC == 0)].index, inplace=True)

        test = pd.DataFrame(testdata.loc[index].values.reshape(1, -1))
        test.columns = ["lnNSAF", "SpC"]
        test.fillna(1, inplace=True)
        test.replace(' NaN', 1, inplace=True)
        test.drop(test[(test.lnNSAF == 1) | (test.SpC == 1) | (test.lnNSAF == 0) | (test.SpC == 0)].index, inplace=True)

        traindatalist.append(foresttrain)
        testdatalist.append(test)

    # This is where the forest is constructed and executed
    for count, foresttype in enumerate(forestinit):

        if usertypes[count] != 0:

            print("Currently Conducting: " + possibletypes[count])

            file = open('src/interface/proteinIDs.txt', 'w')
            file.write(str(len(testdatalist)))
            file.close()

            if possibletypes[count] == "KNeighbors":
                path = 'src/interface/K.txt'
            if possibletypes[count] == "Extra Trees":
                path = 'src/interface/Extra.txt'
            if possibletypes[count] == "Random Forest":
                path = 'src/interface/Random.txt'
            if possibletypes[count] == "Decision Tree":
                path = 'src/interface/Decision.txt'

            testsize = testdata.shape[0]
            testindexes = [i for i in range(len(testdatalist))]
            totaloutput = np.zeros(12)
            for i in testindexes:
                totaloutput = totaloutput + performClassifierProcess(testdatalist[i], traindatalist[i], foresttype, testsize, i, path)
            forestcollection.append(list(map(lambda x: x / int(testdata.shape[0]), totaloutput)))

        else:
            continue

    print(forestcollection)
    plt.clf()

    for i, test in enumerate(forestcollection):
        plt.bar(np.arange(len(test)), test, widths[i], alpha=0.75, color=colours[i])
        plt.xticks(np.arange(len(test)),
                   ['R{}'.format(i + 1) for i in range(len(test))])
        axes = plt.gca()
        axes.set_ylim([0, 1])
    plt.legend(possibletypes)
    plt.plot(np.arange(12), np.average(np.asarray(forestcollection), axis=0), c='black', linewidth=2)
    plt.plot([0, 11], [.5, .5], '--', c='red', linewidth=1)
    plt.savefig(outputfolder + "/MultiLabel/Deviation.png")


def performClassifierProcess(testdatalist, traindatalist, foresttype, size, testnum, path):

    print(str(testnum/size))

    try:
        foresttype.fit(traindatalist.loc[:, "lnNSAF":"SpC"], traindatalist.loc[:, "Label 1":"Label 12"])
        prediction = pd.DataFrame(foresttype.predict(testdatalist)).apply(lambda x: int(x))
        file = open(path, 'w')
        file.write(str(testnum/size))
        file.close()
        return np.array(prediction)
    except ValueError:
        print("Warning: Incompatible Test @: " + str(testnum))
        return np.zeros((12,), dtype=int)


def processingArguments(arg):

    tt = []
    out400 = sys.argv[2].replace(',', '')  # 400 Output folder
    direc6 = sys.argv[1].replace(',', '')  # 6by6 Output folder
    perm = sys.argv[3].replace(',', '')  # PerCat Output folder
    result = sys.argv[4].replace(',', '')  # Folder for All Machine leaning outputs
    for i in sys.argv[5:12]:
        tt.append(int(i.replace(',', '')))  # What tests to perform
    lookID = open('src/interface/Lookfor.txt', 'r').readline()
    if lookID != "[]":
        print(lookID)
        lookout = lookID.replace('[', '').replace(']', '').split(',')
        lookout = list(map(lambda x: x.replace("' ", ""), lookout))
        lookout = list(map(lambda x: x.replace("'", ""), lookout))
        lookout = list(map(lambda x: x.replace(" ", ""), lookout))
    else:
        lookout = ["All"]

    return tt, out400, direc6, perm, result, lookout

if __name__ == '__main__':
    time = str(dt.datetime.now().strftime("%Y-%m-%d_%H-%M-%S"))

    testtypes, outputdirec400, outputdirec6, outputdirecperm, outputresults, lookoutIDs = \
        processingArguments(sys.argv)

    Kerberusdataln, Kerberusdatasp, Testdata, Labels = \
        importCSV(outputdirec400, str(outputdirec6 + "/result.csv"), str(outputdirecperm +"/permtotal.csv"))

    if sum(testtypes[0:4]) > 0:
        multiLabelMultiClassifier(Kerberusdataln, Kerberusdatasp, Testdata, Labels, testtypes[0:4], outputresults)
    if sum(testtypes[4:6]) > 0:
        os.makedirs(outputresults + 'Individual/')
    if testtypes[4] == 1:
        individualForests(Kerberusdataln, Kerberusdatasp, Testdata, "RandomClassifier", time, lookoutIDs, outputresults)
    if testtypes[5] == 1:
        individualForests(Kerberusdataln, Kerberusdatasp, Testdata, "Isolation", time, lookoutIDs, outputresults)
    if testtypes[6] == 1:
        TotalDF, TestDF = constructDataFrames(Kerberusdataln, Kerberusdatasp, Testdata)
        megaIsolationForest(TotalDF, TestDF)

