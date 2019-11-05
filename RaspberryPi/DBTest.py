import pymysql

conn = pymysql.connect(host='192.168.0.7', port=3306, user='root', passwd = 'passwd', db='SmartCage')

cur = conn.cursor()
cur.execute("SELECT feedtime, feedleft FROM cage WHERE id='jinhak94';")

print(cur.description)
print()

for row in cur:
    print(row)

cur.close()
conn.close()
