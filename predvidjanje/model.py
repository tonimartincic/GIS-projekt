import torch.nn as nn
import torch


class LSTMModel(nn.Module):

    def __init__(self, input_size, hidden_size, num_layers, bidirectional=False, activation_fn=nn.functional.relu,
                 batch_first=True):
        super(LSTMModel, self).__init__()

        self.activation_fn = activation_fn

        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, bidirectional=bidirectional, batch_first=False)
        self.fc = nn.Linear(hidden_size, 2)

    def forward(self, x):
        x = torch.transpose(x, 1, 0)  # time-first format
        x, _ = self.lstm(x)
        return self.fc(x[-1])
