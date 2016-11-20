All data is formated as following:
[[[rec_nd_id],[active_status|id1-id2-id3-...|rep_num],...],...]
where
rec_nd_id: user's id who receives message
active_status: user's active status after he received message
id1-id2-id3-...: senders' id, using symbol '-' splitted each sender
rep_num: pattern's repeat number. For example, id1-id2-id3 send message to rec_nd_id and the sending pattern was happened twice during the observation, we use rep_num=2 to record this.

Note that, id with "*" presenting the actual retweeted user.