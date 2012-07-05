# blog

My barebones blog. 

## Usage

If you use cake, substitute 'lein' with 'cake' below. Everything should work fine.

```bash
lein deps
lein run
```

to make a deployable war run

```bash
lein ring uberwar
```

for the really lazy, just grab the standalone jar and run it :)

```bash
wget https://github.com/yogthos/yuggoth/raw/master/yuggoth-0.1.0-SNAPSHOT-standalone.jar
java -jar yuggoth-0.1.0-SNAPSHOT-standalone.jar
```

see [markdown-clj](https://github.com/yogthos/markdown-clj) for supported syntax in posts, any valid HTML will work as well


## License

Copyright (C) 2011 Yogthos

Distributed under the Eclipse Public License, the same as Clojure.

