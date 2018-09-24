((nil . ((cider-ns-refresh-before-fn . "user/stop")
         (cider-ns-refresh-after-fn  . "user/go")
         (eval . (with-eval-after-load 'clojure-mode
                   (setq
                    cider-default-cljs-repl 'figwheel
                    clojure-use-metadata-for-privacy t))))))
