import random

def tr_ts_split(path,ts_ratio=0.1):

    of_tr = open(path[:-7]+"tr.dat","w")
    of_ts = open(path[:-7]+"ts.dat","w")

    with open(path,'r') as f:
        for line in f:
            line = line.replace("*","")
            ls = line[:-1].split(",")
            if len(ls) == 2:
                #only one context appears
                #only save it or training
                of_tr.write(line)
            elif len(ls) <= 1/ts_ratio+1:
                #provide one context for testing is enough
                ts_index = random.randint(1,len(ls)-1)
                of_ts.write(ls[0]+","+ls[ts_index]+"\n")
                row_tr = ls[0]
                for i in xrange(1,len(ls)):
                    if i!=ts_index:
                        row_tr += ","+ls[i]
                of_tr.write(row_tr+"\n")
            else:
                num_ts_context = int(ts_ratio*(len(ls)-1))
                ts_indexes = set(random.sample(range(1,len(ls)-1),num_ts_context))

                row_tr = ls[0]
                row_ts = ls[0]

                for i in xrange(1,len(ls)):
                    if i in ts_indexes:
                        row_ts += "," + ls[i]
                    else:
                        row_tr += "," + ls[i]

                of_tr.write(row_tr+"\n")
                of_ts.write(row_ts+"\n")

            #break


if __name__ == '__main__':
    path = "../data/toy.dat"
    tr_ts_split(path,ts_ratio=0.1)
