import torch
import torch.utils.data as data

import csv
import numpy as np
import datetime

import model
import matplotlib.pyplot as plt


def evaluate(model, loader, criterion, device):
    model.eval()
    with torch.no_grad():
        loss = 0.0
        for idx, batch in enumerate(loader):
            x, y = batch
            x, y = x.to(device), y.to(device)

            pred = model(x)
            loss += criterion(pred, y).item()

        return loss / len(loader)


def poly_area(x, y):
    return 0.5*np.abs(np.dot(x, np.roll(y, 1))-np.dot(y, np.roll(x, 1)))


class LightningPolyDataset(data.Dataset):

    def __init__(self, filepath, mode='train'):
        self.instances = []
        self.labels = []
        self.mode = mode

        with open(filepath) as csvfile:
            csv_reader = csv.reader(csvfile, delimiter=',')
            start = 0 if mode == 'train' else 3 * 3000
            end = 3*300 if mode == 'train' else 3*3501
            reader_en = enumerate(csv_reader, start=-start)
            next(reader_en)
            for i in range(start):
                next(reader_en)
            try:
                #if mode == 'train':
                while start < end:
                    start += 1
                    for i in range(4):
                        _, row = next(reader_en)
                        if int(row[2]) != 0:  # if timeid is not 0 (now)
                            # parse stroke count
                            stroke_cnt = 0
                            for poly_str_cnt in row[7].split():
                                stroke_cnt += int(poly_str_cnt)

                            # parse centroid
                            x, y = float(row[4]), float(row[5])

                            # parse area
                            area = float(row[8])

                            # parse altitude
                            altitude = float(row[10])

                            # parse timesptamp
                            date = datetime.datetime.strptime(row[9], '%Y-%m-%dT%H:%M')
                            instance = np.array([x, y])
                            self.instances.append(torch.FloatTensor(instance))
                        else:
                            self.labels.append(torch.FloatTensor([float(row[4]), float(row[5])]))
                #elif mode == 'test':
                    '''
                    _, row = next(reader_en)
                    # parse stroke count
                    stroke_cnt = 0
                    for poly_str_cnt in row[7].split():
                        stroke_cnt += int(poly_str_cnt)

                    # parse centroid
                    x, y = float(row[4]), float(row[5])

                    # parse area
                    area = float(row[8])

                    # parse altitude
                    altitude = float(row[10])

                    # parse timestamp
                    date = datetime.datetime.strptime(row[9], '%Y-%m-%dT%H:%M')
                    instance = np.array([x, y])
                    self.instances.append(torch.FloatTensor(instance))
                    while True:
                        _, row = next(reader_en)
                        # parse stroke count
                        stroke_cnt = 0
                        for poly_str_cnt in row[7].split():
                            stroke_cnt += int(poly_str_cnt)

                        # parse centroid
                        x, y = float(row[4]), float(row[5])
                        self.labels.append(torch.FloatTensor([x, y]))

                        # parse area
                        area = float(row[8])

                        # parse altiude
                        altidue = float(row[10])

                        # parse timesptamp
                        date = datetime.datetime.strptime(row[9], '%Y-%m-%dT%H:%M')
                        instance = np.array([x, y])
                        self.instances.append(torch.FloatTensor(instance))'''

            except StopIteration:
                print()

    def __len__(self):
        return len(self.labels)

    def __getitem__(self, idx):
        return torch.cat((self.instances[idx*3], self.instances[idx*3+1], self.instances[idx*3+2])), self.labels[idx]


if __name__=='__main__':
    dataset_train = LightningPolyDataset("res/podaci-za-ucenje.xls")
    dataset_test = LightningPolyDataset("res/podaci-za-ucenje.xls", mode='test')
    dataloader = data.DataLoader(dataset_train, shuffle=True, batch_size=64)
    dataloader_test = data.DataLoader(dataset_test, batch_size=512)
    device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')

    my_model = model.LSTMModel(2, 10, 1)
    my_model.to(device)
    my_model.train()
    criterion = torch.nn.SmoothL1Loss()
    mse = torch.nn.MSELoss()
    optimizer = torch.optim.SGD(my_model.parameters(), lr=0.0001)
    lr_scheduler = torch.optim.lr_scheduler.ExponentialLR(optimizer, gamma=0.95)
    train_mse_error = []
    test_mse_error = []
    lrs = []
    for epoch in range(2000):
        total_loss = 0.0
        mse_total_loss = 0.0
        for idx, batch in enumerate(dataloader):
            x, y = batch
            x, y = x.to(device), y.to(device)
            pred = my_model(x)
            loss = mse(pred, y)
            total_loss += loss.item()
            loss.backward()
            optimizer.step()

        test_loss = evaluate(my_model, dataloader_test, criterion, device)
        print('epoch:', epoch, ' | train loss:', total_loss / len(dataloader),
              ' | test loss: ', test_loss)
        train_mse_error.append(total_loss / len(dataloader))
        test_mse_error.append(test_loss)
        lrs.append(lr_scheduler.get_last_lr()[0])
        my_model.train()
        if epoch < 500:
            lr_scheduler.step()

    torch.save(my_model.state_dict(), './res/lstm.pt')
    plt.plot(train_mse_error)
    plt.ylabel('MSE')
    plt.xlabel('Epoch')
    plt.title('Train MSE error')
    plt.show()

    plt.plot(test_mse_error)
    plt.ylabel('MSE')
    plt.xlabel('Epoch')
    plt.title('Test MSE error')
    plt.show()

    plt.plot(lrs)
    plt.xlabel('Epoch')
    plt.ylabel('Learning rate')
    plt.show()


