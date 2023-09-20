# defining the libraries
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

algos = ['flexcast', 'byzcast', 'skeen']

for algo in algos:
    data = pd.read_csv('CDF_'+algo+'_99%loc_node1.txt')
    # data = np.percentile(data,1)

    # getting data of the histogram
    count, bins_count = np.histogram(data, bins=1000)
    # finding the PDF of the histogram using count values
    pdf = count / sum(count)
    # using numpy np.cumsum to calculate the CDF
    # We can also find using the PDF values by looping and adding
    cdf = np.cumsum(pdf)
    
    # plotting PDF and CDF
    # plt.plot(bins_count[1:], pdf, color="red", label="PDF")
    plt.plot(bins_count[1:], cdf, label=algo)


plt.legend()
plt.show()