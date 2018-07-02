from functools import reduce
import pandas as pd
import glob
import numpy as np
import seaborn as sea
import matplotlib.pyplot as plt
from sklearn.ensemble import IsolationForest


def cleanData(identity, columns):

    imports = [pd.read_csv(file, delimiter=',', header=None, names=['ID', 'ttest'], usecols=columns, quotechar='"')
               for file in identity]

    outerjoin = reduce(lambda a, b: pd.merge(a, b, on='ID', how='outer'), imports)
    outerjoin.set_index('ID', drop=True, inplace=True)
    outerjoin.columns = [str(i+1) for i in range(0, len(outerjoin.columns))]
    outerjoin = outerjoin.loc[:, '1':'400'].apply(pd.to_numeric, errors='coerce')
    outerjoin.fillna(1, inplace=True)
    outerjoin.replace(0, 1, inplace=True)

    return outerjoin


def importCSV(kfilepath, tfilepath):

    print("Importing CSV files...")

    ids = glob.glob(kfilepath + "*.csv")
    outerjoinln = cleanData(ids, [0, 1])
    outerjoinsp = cleanData(ids, [0, 2])

    test = pd.read_csv(tfilepath, delimiter=',', header=None, names=['ID', 'lnnsaf', 'spc'], quotechar='"')
    test.set_index('ID', drop=True, inplace=True)

    return outerjoinln, outerjoinsp, test


def constructDataFrames(kerberusln, kerberussp, testdata):

    print("Making the Train and Test Dataframes...")

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


def individualIsolationForests(kerberusln, kerberussp, testdata):

    count = 0
    predictiontest = []
    forest = IsolationForest(max_samples='auto', contamination=0.2)

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
            forest.fit(foresttrain)
            predictiontest.append(forest.predict(test))  # This tells us whether the test is an outlier/in
        except ValueError:
            print("Warning: Incompatible Test @: " + str(count))
            test.to_csv(str("src/output/error/" + str(count) + "-Test.csv"), sep=',')
            foresttrain.to_csv(str("src/output/error/" + str(count) + "-Train.csv"), sep=',')
            continue

        plt.clf()
        index = index.replace("|", "-")
        plt.title(index + " Isolation Forest")
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
        plt.savefig('src/output/Indie-Isolation/' + index + '.jpg')
        plt.close("all")


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


if __name__ == '__main__':

    Kerberusdataln, Kerberusdatasp, Testdata = importCSV("src/output/400/", "src/output/6by6/result.csv")
    individualIsolationForests(Kerberusdataln, Kerberusdatasp, Testdata)
    TotalDF, TestDF = constructDataFrames(Kerberusdataln, Kerberusdatasp, Testdata)
    megaIsolationForest(TotalDF, TestDF)

