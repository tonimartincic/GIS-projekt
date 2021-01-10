from math import e, pi, sqrt


'''
Klasa koja predstavlja poligon
'''
class Polygon:

    def __init__(self, points):
        self.points = points

    def __str__(self):
        string = "Polygon with points [ "
        for point in self.points:
            string +="( " +str(point)+" );"
        string += " ]"
        return string


'''
Klasa koja predstavlja tocku
'''
class Point:
    def __init__(self, longitude, latitude):
        self.longitude = longitude
        self.latitude = latitude

    def __str__(self):
        return "Point Latitude: "+str(self.latitude)+" ,Longitude: "+str(self.longitude)


'''
Metoda za izracun DFT-a (dobivanje skupa koeficijenata iz tocaka)
'''
def DFT(polygon):
    points = polygon.points
    N = len(points)
    Z = []
    for k in range(0, N):
        sum = 0
        for m in range(0, N):
            point = points[m]
            z = complex(point.latitude, point.longitude)
            c = e ** (complex(0, -2 * pi * m * k / N))
            sum += z * c
        Zk = sum/N
        Z.append(Zk)
    return Z


'''
Metoda za izracun inverznog DFT-a (dobivanje skupa tocaka iz koeficijenata)
'''
def InverseDFT(Z, M):
    N = len(Z)
    z = []
    for m in range(0, N):
        sum = 0
        lbound = int(-M/2)
        if(M % 2 == 0):
            ubound = int(M/2)
        else:
            ubound = int(M/2)+1

        for k in range(lbound, ubound):
            index = k % N
            Zk = Z[index]
            c = e ** (complex(0, 2 * pi * m * k / N))
            sum += Zk * c
        z.append(sum)
    return z


'''
Metoda za pretvorbu izlaza inverznog DFT-a u objekt poligona
'''
def getPoygonFromInverseDFT(z):
    points = []
    for zk in z:
        latitude = zk.real
        longitude = zk.imag
        p = Point(longitude, latitude)
        points.append(p)
    polygon = Polygon(points)
    return polygon


'''
Metoda za izracun razlike izmedu 2 poligona kao suma Eulerovih udaljenosti parova tocaka
podijeljena s brojem tocaka
'''
def calculateDiffrence(polygon1, polygon2):
    points1 = polygon1.points
    points2 = polygon2.points
    sum = 0
    for i in range(0, len(points1)):
        point1 = points1[i]
        point2 = points2[i]
        diffLon = point1.longitude - point2.longitude
        diffLat = point1.latitude - point2.latitude
        sum += sqrt(diffLon * diffLon + diffLat * diffLat)
    return sum/len(points1)


if __name__ == '__main__':
    fileName = "res/cluster.csv"
    file = open(fileName, 'r')

    '''
    Parsiraj datoteku sa zapisom o poligonima i stvori objekte poligona i tocaka
    '''
    lines = file.readlines()
    counter = 0
    flag = False
    polygons = []
    for line in lines:
        if not flag:
            flag = True
        elif flag:
            splits = line.split('POLYGON')
            splits2 = splits[1].split(')')
            pointList = splits2[0][3:-1].split(", ")
            points = []
            for point in pointList:
                coordinates = point.split(" ")
                p = Point(float(coordinates[0]), float(coordinates[1]))
                points.append(p)
            pol = Polygon(points)
            polygons.append(pol)
            counter += 1
            if counter >= 100:
                break

    '''
    Za svaki poligon izracunaj DFT koeficijente i sa M koeficijenata izracunaj IDFT kako bi dobio
    rekonstruirane koordinate poligona, izracunaj gresku izmedu dobivenog i originalnog poligona
    '''
    for polygon in polygons:
        print(polygon)
        Z = DFT(polygon)
        M = len(polygon.points)/2
        z = InverseDFT(Z, M)
        polygonReconstruct = getPoygonFromInverseDFT(z)
        print(polygonReconstruct)
        diff = calculateDiffrence(polygon, polygonReconstruct)
        print("Difference = " + str(diff))
        print("----------------------------------------------------")

    file.close()
