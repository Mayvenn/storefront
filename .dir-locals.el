((nil . ((cider-ns-refresh-before-fn . "user/stop")
         (cider-ns-refresh-after-fn  . "user/go")
         (cider-lein-parameters . "with-profile +dev repl :headless :host localhost")
         (eval . (with-eval-after-load 'clojure-mode
                   (setq
                    cider-default-cljs-repl 'figwheel-main
                    clojure-use-metadata-for-privacy t))))))
