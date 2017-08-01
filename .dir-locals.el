(setq cider-cljs-lein-repl "(do (use 'figwheel-sidecar.repl-api) (start-figwheel!) (cljs-repl))")
(setq cider-refresh-before-fn "user/stop" cider-refresh-after-fn "user/go")

(setq cider-refresh-show-log-buffer t)

