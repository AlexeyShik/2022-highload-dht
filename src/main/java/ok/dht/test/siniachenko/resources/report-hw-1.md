# Использованная БД

Вначале я скопировал референсную реализацию и воспользовался ею. Но профилировщик показывал очень большую долю, занятую
GC1, рознилось от 60 до примерно 85 процентов всего профиля. И ещё часто бывали ошибки OOM, которые лечились уменьшением
нагрузки. В итоге я решил взять LevelDB. С ней таких проблем на профиле не находилось, но это отчасти потому, что часть
её реализации написана на C++ и не идёт в наши джавовые профили.

# Использованные команды wrk

Использовал команду `wrk -d $1 -t 1 -c 1 -R $2 -s put-request.lua http://localhost:12345` для PUT, в параметры передавал
длительность и rps. lua скрипт:
```
request = function()
    key = math.random(1000000)
    value = math.random(1000000)
    path = "/v0/entity?id=key-" .. key
    wrk.body = "value-" .. value
    return wrk.format("PUT", path)
end
```
А для GET использовал команду `wrk -d $1 -t 1 -c 1 -R $2 -s get-request.lua http://localhost:12345`, в параметры
передавал то же самое. lua скрипт:
```
request = function()
    key = math.random(1000000)
    path = "/v0/entity?id=key-" .. key
    return wrk.format("GET", path)
end
```

# Нахождение стабильной нагрузки

Вначале начал искать границу стабильной нагрузки PUT запросами. Попробовал `put.sh 120 15000` (2 минуты по 15000 rps):
```
Running 2m test @ http://localhost:12345
  1 threads and 1 connections
  Thread calibration: mean lat.: 3.298ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    10.15ms   57.90ms 489.47ms   97.20%
    Req/Sec    15.82k     1.59k   23.67k    82.86%
  1799989 requests in 2.00m, 115.01MB read
Requests/sec:  14999.93
Transfer/sec:      0.96MB
```

Сервер отлично отработал такую нагрузку. Попробовал увеличить до `put.sh 120 20000`:
```
Running 2m test @ http://localhost:12345
  1 threads and 1 connections
  Thread calibration: mean lat.: 253.073ms, rate sampling interval: 1037ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.37s   907.49ms   3.06s    53.48%
    Req/Sec    19.57k     1.38k   21.05k    87.74%
  2338862 requests in 2.00m, 149.44MB read
Requests/sec:  19490.54
Transfer/sec:      1.25MB
```

Заметно выросла latency, в 100 раз, думаю, что из-за заполненности базы. Но при этом сервер успевал почти все запросы
выполнять. Терял 500 запросов из 20000. Кажется, близко подошли к границе. Я решил попробовать сильно увеличить,
до `put.sh 120 50000`:
```
Running 2m test @ http://localhost:12345
  1 threads and 1 connections
  Thread calibration: mean lat.: 3045.437ms, rate sampling interval: 10919ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    39.43s    19.19s    1.20m    57.53%
    Req/Sec    19.90k   422.06    20.32k    90.00%
  2387077 requests in 2.00m, 152.53MB read
Requests/sec:  19892.35
Transfer/sec:      1.27MB
```
latency снова выросла, думаю по той же причине. Сервер уже захлёбывался в запросах. успевал только 20000 из 50000
обрабатывать, то есть предыдущая граница была очень точной. После этого попробовал GET запросы с таким же рейтом,
`get.sh 120 20000`:
```
Running 2m test @ http://localhost:12345
  1 threads and 1 connections
  Thread calibration: mean lat.: 1208.848ms, rate sampling interval: 3416ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    10.26s     4.26s   18.22s    64.82%
    Req/Sec    17.02k     1.31k   18.29k    84.38%
  2035542 requests in 2.00m, 144.85MB read
  Non-2xx or 3xx responses: 93038
Requests/sec:  16962.88
Transfer/sec:      1.21MB
```

Сервер успевал уже только 17000 из 20000, граница похуже, но всё равно близка. Интересно, почему на GET запросах сервер
меньше запросов успевал обрабатывать, чем на PUT. Думаю, это потому, что ему нужно отдавать байты значений, запись этих
байтов и занимала дольше времени.

# Результаты профилировки CPU

В целом, для всех 4 тестов профили довольны похожи по соотношениям трейсов.
Везде в самом начале есть промежуток
полусекунды-секунды, где есть компиляция. Логично, что большой выделенный кусок только в первом тесте, когда сервак
только начал принимать PUT запросы, и в последнем, когда впервые пошла обработка GET запросов.
Видны во всех местах одинокие тёмно-красные точки, там работал GC.
Так же периодически появляются красно-розовые полосы длительность в секунду-две, там происходила компиляция чего-то.
Интервалы между этими полосами примерно одинаковые, 40-50 секунд. Что интересно, в GET тесте такого не было. Там зато
в самом начале была жирная полоска компиляции, а потом одна маленькая, а все остальное время компиляций почти вообще
не было. Скорее всего, какие-то особенности get метода у базы.

В целом видно, что большую часть времени занимают системные вызовы `read`, `write` и `kevent`. Их никуда не деть, они
в любом случае будут, их не ускорить. Помимо них чуть меньше времени занимала java компилятором и сборщиком мусора.
И остальное уже преимущественно база.
Выглядит так, что изменить эту картину можно только через распараллеливание обработки запросов и возможность
одновременной записи в и чтения из сокетов.

# Результаты профилирования аллокаций

Тут профиль интересен тем, что для PUT запросов основной фон почти белый, еле розовый, то есть, видимо, аллокаций
довольно мало. Непонятно только, относительно чего мало. Видимо, мало относительно тёмно-красных участков. А с GET
фон профиля бежево-розовый, там уже больше. И судя по описанию, больше аллокаций из-за аллокаций `byte[]` в конструкторе
Slice, вызываемом в DbImpl::get.
В PUT профилях снова есть полоски, они попадают в те же промежутки, что и полоски в CPU профиле. По трейсу видно, что
в базе делается background compaction, перекидываются данные с памяти на диск. Логично, что это и вызывает сборщик
мусора в CPU профиле, потому что джавовые объекты, хранившие эти данные, больше не нужны.
В GET такого нет, потому что данные не меняются.
Так же видны отдельные красные точки, там тоже проводилось сжатие данных базы в оперативке.

В целом, в нашем сервисе аллокации хорошо поразбиты по трейсу, нет больших выделяющихся кусков. Аллокации происходят
из-за копирования данных из строки значения id в массив байт, и из-за постоянного создания объектов Response. В самом
начале я попробовал все одинаковые Response объекты вынести в константы. Но потом выяснилось, что так нельзя, они
изменяемые и one-nio это использует, дописывая в них http заголовки.
