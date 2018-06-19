from functools import reduce
import pandas as pd
import glob


def importCSV(filepath):

    ids = glob.glob(filepath + "*.csv")
    allimports = [pd.read_csv(file, delimiter=',', header=None, names=['ID', 'ttest'], quotechar='"') for file in ids]
    outerjoin = reduce(lambda a, b: pd.merge(a, b, on='ID', how='outer'), allimports)
    outerjoin.set_index('ID', drop=True, inplace=True)
    outerjoin.columns = [str(i+1) for i in range(0, len(outerjoin.columns))]
    outerjoin.fillna(1, inplace=True)


if __name__ == '__main__':

    importCSV("src/output/")
