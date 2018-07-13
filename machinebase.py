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


def individualForests(kerberusln, kerberussp, testdata, type):

    print("Producing " + type + " forests")

    count = 0
    predictiontest = []
    if type == "Isolation":
        forest = IsolationForest(max_samples='auto', contamination=0.2)
        foldername = 'src/output/isolationforest/'
    elif type == "RandomClassifier":
        forest = RandomForestClassifier(n_estimators=100)
        foldername = 'src/output/randomclassifierforest/'
        categories = pd.read_csv('src/output/permcategories/permcategories.csv', delimiter=',', header=None)

    # This section here will produce 400 separate forests, 1 for every ttest combo,
    # indexed to a protein ID

    for index, row in testdata.iterrows():  # index = ID and row = all 400 values
        count += 1

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
            print("Tree " + str(count) + " of " + str(len(testdata.index)) + " completed.", end="\r", flush=True)
            if type == "Isolation":
                forest.fit(foresttrain)
                predictiontest.append(forest.predict(test))  # This tells us whether the test is an outlier/in
            elif type == "RandomClassifier":
                forest.fit(foresttrain, foresttrain)
                predictiontest.append(forest.predict(test))
        except ValueError:
            print("Warning: Incompatible Test @: " + str(count))
            test.to_csv(str("src/output/error/" + str(count) + "-Test.csv"), sep=',')
            foresttrain.to_csv(str("src/output/error/" + str(count) + "-Train.csv"), sep=',')
            continue

        plt.clf()
        index = index.replace("|", "-")
        plt.title(index + " " + type + " Isolation Forest")
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
    totaldf.drop(totaldf[(totaldf.lnNSAF == 1) | (totaldf.SpC == 1) |
                         (totaldf.lnNSAF == 0) | (totaldf.SpC == 0) |
                         (totaldf.lnNSAF.dtype == np.str) | (totaldf.SpC.dtype == np.str)].index, inplace=True)
    totaldf.reset_index(drop=True, inplace=True)

    testdf = testdata
    testdf.columns = ["lnNSAF", "SpC"]
    testdf.drop(testdf[(testdf.lnNSAF == 1) | (testdf.SpC == 1) |
                       (testdf.lnNSAF == 0) | (testdf.SpC == 0)].index, inplace=True)
    testdf.reset_index(drop=True, inplace=True)

    return totaldf, testdf


def megaIsolationForest(totaldf, testdf):

    forest = IsolationForest(max_samples='auto', contamination=0.2)

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


def mLabelRandomForestClassifier(kerberusln, kerberussp, testdata, labels):

    forestcollection = []
    types = ["KNeighbors", "Extra Trees", "Random Forest", "Decision Tree"]
    widths = [0.8, 0.65, 0.5, 0.35]

    for count, foresttype in enumerate([
        KNeighborsClassifier(n_jobs=-1),
        ExtraTreesClassifier(testdata.shape[0], n_jobs=-1),
        RandomForestClassifier(testdata.shape[0], n_jobs=-1),
        DecisionTreeClassifier()]):

        print("Currently Conducting: " + types[count])
        forestarray = np.zeros(12)

        for count, (index, row) in enumerate(testdata.iterrows()):  # index = ID and row = all 400 values

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

            forest = foresttype
            try:
                forest.fit(foresttrain.loc[:, "lnNSAF":"SpC"], foresttrain.loc[:, "Label 1":"Label 12"])
                prediction = pd.DataFrame(forest.predict(test)).apply(lambda x: int(x)) # KNeighbours and Decision Trees
                #  output a string list, so this piece of code here standardises the different tests
                forestarray = forestarray + np.array(prediction)
                print(str(count + 1) + " of " + str(testdata.shape[0]) + " completed")
            except ValueError:
                print("Warning: Incompatible Test @: " + str(count + 1))
                continue

        forestcollection.append(list(map(lambda x: x / int(testdata.shape[0]), forestarray)))

    print(forestcollection)
    plt.clf()

    for i, test in enumerate(forestcollection):
        plt.bar(np.arange(len(test)), test, widths[i], alpha=0.9)
        plt.xticks(np.arange(len(test)),
                   ['R{}'.format(i + 1) for i in range(len(test))])
        axes = plt.gca()
        axes.set_ylim([0, 1])
    plt.legend(types)
    plt.plot(np.arange(12), np.average(np.asarray(forestcollection), axis=0))
    plt.plot([0, 11], [.5, .5], '--', c='black', linewidth=2)
    plt.show()


if __name__ == '__main__':

    Kerberusdataln, Kerberusdatasp, Testdata, Labels = importCSV("src/output/400/", "src/output/6by6/result.csv",
                                                                 "src/output/permcategories/permtotal.csv")
    mLabelRandomForestClassifier(Kerberusdataln, Kerberusdatasp, Testdata, Labels)
    individualForests(Kerberusdataln, Kerberusdatasp, Testdata, "RandomClassifier")
    individualForests(Kerberusdataln, Kerberusdatasp, Testdata, "Isolation")
    TotalDF, TestDF = constructDataFrames(Kerberusdataln, Kerberusdatasp, Testdata)
    megaIsolationForest(TotalDF, TestDF)

