import subprocess
import random
from pprint import pprint

results = dict();
teams = random.sample(range(8), 3)
subprocess.call(['java', 'pppp.sim.Simulator', '--turns', '100000', '--pipers', '6', '--rats', '100','--groups', 'g9', 'g' + str(teams[0]) , 'g' + str(teams[1]), 'g' + str(teams[2])])
