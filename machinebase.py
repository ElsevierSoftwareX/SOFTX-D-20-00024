from functools import reduce
import pandas as pd
import glob
import numpy as np
import matplotlib.pyplot as plt
from sklearn.ensemble import IsolationForest


def importCSV(kfilepath, tfilepath):

    ids = glob.glob(kfilepath + "*.csv")
    allimports = [pd.read_csv(file, delimiter=',', header=None, names=['ID', 'ttest'], usecols=[0, 1], quotechar='"')
                  for file in ids]
    outerjoinln = reduce(lambda a, b: pd.merge(a, b, on='ID', how='outer'), allimports)
    outerjoinln.set_index('ID', drop=True, inplace=True)
    outerjoinln.columns = [str(i+1) for i in range(0, len(outerjoinln.columns))]
    outerjoinln.fillna(1, inplace=True)
    outerjoinln.replace(0, 1, inplace=True)

    allimports = [pd.read_csv(file, delimiter=',', header=None, names=['ID', 'ttest'], usecols=[0, 2], quotechar='"')
                  for file in ids]
    outerjoinsp = reduce(lambda a, b: pd.merge(a, b, on='ID', how='outer'), allimports)
    outerjoinsp.set_index('ID', drop=True, inplace=True)
    outerjoinsp.columns = [str(i+1) for i in range(0, len(outerjoinsp.columns))]
    outerjoinsp.fillna(1, inplace=True)
    outerjoinsp.replace(0, 1, inplace=True)

    test = pd.read_csv(tfilepath, delimiter=',', header=None, names=['ID', 'lnnsaf', 'spc'], quotechar='"')
    test.set_index('ID', drop=True, inplace=True)

    return outerjoinln, outerjoinsp, test

def isolationForest(kerberusln, kerberussp, testdata):

    count = 0

    for index, row in testdata.iterrows():  # index = ID and row = all 400 values
        count += 1

        if index not in kerberusln.index.values:
            print("Test not in 400Data...? Why?")
            continue

        foresttrain = pd.concat([kerberusln.loc[index], kerberussp.loc[index]], axis=1)
        foresttrain.columns = ["lnNSAF", "SpC"]
        foresttrain.fillna(1, inplace=True)
        foresttrain.replace(' NaN', 1, inplace=True)
        #foresttrain = foresttrain[~(foresttrain < 1).all(1)]

        test = pd.DataFrame(testdata.loc[index].values.reshape(1, -1))
        test.columns = ["lnNSAF", "SpC"]
        test.fillna(1, inplace=True)
        test.replace(' NaN', 1, inplace=True)

        forest = IsolationForest(max_samples='auto', contamination=0.2)

        try:
            forest.fit(foresttrain)
            prediction_train = forest.predict(foresttrain)  # This gives us an array where all of the data is assigned either outlier/in
            prediction_test = forest.predict(test) # This tells us whether the test is an outlier/in
        except ValueError:
            test.to_csv(str("src/output/error/" + str(count) + "-Test.csv"), sep=',')
            foresttrain.to_csv(str("src/output/error/" + str(count) + "-Train.csv"), sep=',')
            continue

        xx, yy = np.meshgrid(np.linspace(0, 1, 50), np.linspace(0, 1, 50))
        z = forest.decision_function(np.c_[xx.ravel(), yy.ravel()])
        z = z.reshape(xx.shape)

        plt.contourf(xx, yy, z, cmap=plt.cm.Reds_r)
        plt.title("Isolation Forest")

        b1 = plt.scatter(foresttrain["lnNSAF"], foresttrain["SpC"], c='white', s=20, edgecolor='k')
        b2 = plt.scatter(test["lnNSAF"], test["SpC"], c='green', s=20, edgecolor='k')
        plt.legend([b1, b2], ["training observations", "new observations"], loc="upper left")
        plt.xlabel("TTest from lnNSAF")
        plt.ylabel("TTest from SpC")
        plt.yticks([])
        plt.xticks(np.arange(0, 1, step=0.05))
        plt.show()
        plt.close("all")


if __name__ == '__main__':

    Kerberusdataln, Kerberusdatasp, Testdata = importCSV("src/output/400/", "src/output/6by6/result.csv")
    isolationForest(Kerberusdataln, Kerberusdatasp, Testdata)
