import sys


def main(database_name):
    data = []
    with open(database_name, newline='') as csvfile:
        data = csvfile.readlines()
    data.pop(0)

    x = []
    y = []

    for i in range(0, len(data)):
        data[i] = data[i].split(',')
    for i in range(len(data)):
        x.append(float(data[i][4]))
        y.append(float(data[i][5]))

    counter_dobrih = 0
    counter_losih = 0
    for i in range(0, len(x), 4):
        if ((x[i] > x[i+1] and x[i+1] > x[i+2]) or (x[i] < x[i+1] and x[i+1] < x[i+2])) and ((y[i] > y[i+1] and (y[i+1] > y[i + 2])) or (y[i] < y[i+1] and y[i+1] < y[i+2])):
            counter_dobrih += 1
            continue

        if ((x[i+1] > x[i+2] and x[i+2] > x[i+3]) or (x[i+1] < x[i+2] and x[i+2] < x[i+3])) and ((y[i+1] > y[i+2] and y[i+2] > y[i+3]) or (y[i+1] < y[i+2] and y[i+2] < y[i+3])):
            counter_dobrih += 1
            continue

        counter_losih += 1

    print("Kontinuiranih n-torki:", counter_dobrih)
    print("Nekontinuiranih n-torki", counter_losih)
    return


if __name__ == '__main__':
    #main('dataset.csv')
    main(sys.argv[1])
    input("Pritisnite tipku za zatvaranje...")
