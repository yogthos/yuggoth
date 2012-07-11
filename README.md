# Yuggoth

My barebones blog. 

# Features

* markdown content editing
* file uploads and management
* custom styles
* content export

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

valid options

* -mode dev or prod
* -port integer


see [markdown-clj](https://github.com/yogthos/markdown-clj) for supported syntax in posts, any valid HTML will work as well


## License

Copyright (C) 2012 Yogthos

Distributed under the Eclipse Public License, the same as Clojure.

