import datetime
import sys

def main(database_name, wantedx, wantedy, range_rad, start, end, wantedid):
    startY = int(start.split('-')[0])
    startM = int(start.split('-')[1])
    startD = int(start.split('-')[2].split('T')[0])
    startHou = int(start.split('-')[2].split('T')[1].split(':')[0])
    startMin = int(start.split('-')[2].split('T')[1].split(':')[1])
    startDate = datetime.datetime(startY, startM, startD, startHou, startMin)

    endY = int(end.split('-')[0])
    endM = int(end.split('-')[1])
    endD = int(end.split('-')[2].split('T')[0])
    endHou = int(end.split('-')[2].split('T')[1].split(':')[0])
    endMin = int(end.split('-')[2].split('T')[1].split(':')[1])
    endDate = datetime.datetime(endY, endM, endD, endHou, endMin)

    data = []
    x = []
    y = []
    strokes = []
    times = []
    alt = []
    area = []
    ids = []
    with open(database_name, newline='') as csvfile:
        data = csvfile.readlines()
    data.pop(0)

    for i in range(0, len(data)):
        data[i] = data[i].split(',')
    for i in range(len(data)):
        x.append(float(data[i][4]))
        y.append(float(data[i][5]))
        stroke = data[i][-4].split(' ')
        strokex = []
        for s in stroke:
            strokex.append(int(s))

        strokes.append(int(sum(strokex)))
        alt.append(float(data[i][-1]))
        times.append((data[i][-2]))
        area.append(float(data[i][-3]))
        ids.append(float(data[i][2]))

    datetimes = []
    for t in times:
        datetimes.append(datetime.datetime(int(t.split('-')[0]), int(t.split('-')[1]), int(t.split('-')[2].split('T')[0]), int(t.split('-')[2].split('T')[1].split(':')[0]), int(t.split('-')[2].split('T')[1].split(':')[1])))

    area_sum = 0
    stroke_sum = 0
    alt_sum = 0
    counter = 0

    for i in range(len(datetimes)):
        if datetimes[i] >= startDate and datetimes[i] <= endDate and abs(wantedx - x[i]) <= range_rad and abs(wantedy - y[i]) <= range_rad and ids[i] == wantedid:
            area_sum += area[i]
            stroke_sum += strokes[i]
            alt_sum += alt[i]
            counter += 1

    if counter == 0:
        input("Nema odgovarajućih unosa...")
        exit(0)
    print("Ukupna površina oblaka:", area_sum)
    print("Prosječna površina oblaka:", area_sum/counter)
    print("Prosječna nadmorska visina:", alt_sum/counter)
    print("Ukupan broj munja:", stroke_sum)
    print("Prosječan broj munja po oblaku:", stroke_sum/counter)
    print("Ukupan broj oblaka:", counter)
    return


if __name__ == '__main__':
    #main('dataset.csv', 1, 1, 300, '2018-02-01T19:00', '2018-04-01T19:00', -2)
    main(sys.argv[1], float(sys.argv[2]), float(sys.argv[3]), float(sys.argv[4]), sys.argv[5], sys.argv[6], float(sys.argv[7]))
    input("Pritisnite bilo koju tipku za izlazak...")