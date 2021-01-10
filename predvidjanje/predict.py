import torch
import torch.nn as nn

from data import LightningPolyDataset
import numpy as np

model = nn.Linear(6, 2)
model.load_state_dict(torch.load('res/fcmodel.pt'))
device = torch.device('cuda')

dataset = LightningPolyDataset("res/podaci-za-ucenje.xls", mode='train')

dataloader = torch.utils.data.DataLoader(dataset, shuffle=False, batch_size=len(dataset))

for idx, batch in enumerate(dataloader):
    x, y = batch
    predict = model(x)
    np.savetxt('res/prediction.csv', predict.detach().cpu().numpy(), delimiter=',', fmt='%f')