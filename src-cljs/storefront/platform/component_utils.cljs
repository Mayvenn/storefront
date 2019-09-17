(ns storefront.platform.component-utils
  (:require [lambdaisland.uri :as uri]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.history :as history]
            [storefront.routes :as routes]
            [storefront.utils.query :as query]
            [storefront.browser.scroll :as scroll]
            [clojure.string :as str]
            [goog.object :as gobj]
            ["react" :as react]))

(defn create-class [super-class init-fn static-properties method-names]
  (let [ctor (fn ^void constructor [props]
               (this-as this
                 ;; auto-bind methods
                 (doseq [[method f] method-names
                         :when f]
                   (gobj/set this (munge method) (partial (.bind f this) this)))

                 ;; move props.options -> this.options instead of props
                 (gobj/set this "options" (gobj/get this "options"))
                 (gobj/remove this "options")

                 ;; call super()
                 (.call super-class this props)

                 (when init-fn
                   (set! (.-state this) #js {:state ((.bind init-fn this) this props)}))
                 this))]
    ;; set static properties on prototype
    (goog/inherits ctor super-class)
    (doseq [[k v] static-properties]
      (gobj/set ctor k v))
    ctor))

(defn create-component [init-fn static-properties method-names]
  (create-class react/Component init-fn static-properties method-names))

(defn create-pure-component [init-fn static-properties method-names]
  (create-class react/PureComponent init-fn static-properties method-names))

(def create-context
  "Just react/createContext"
  react/createContext)

(defn position [pred coll]
  (first (keep-indexed #(when (pred %2) %1)
                       coll)))

(defn noop-callback [e] (.preventDefault e))

(defn send-event-callback
  ([event] (send-event-callback event nil))
  ([event args] (send-event-callback event args nil))
  ([event args {:keys [prevent-default?] :or {prevent-default? true}}]
   (fn [e]
     (when prevent-default?
       (.preventDefault e))
     (handle-message event args)
     nil)))

(defn expand-menu-callback [keypath]
  (send-event-callback events/control-menu-expand {:keypath keypath}))

(defn collapse-menus-callback [menus]
  (send-event-callback events/control-menu-collapse-all {:menus menus}))

(defn fake-href
  ([event] (fake-href event nil))
  ([event args]
   {:href "#"
    :on-click (send-event-callback event args)}))

(defn route-to [navigation-event & [navigation-args nav-stack-item]]
  (if-let [routable-path (routes/path-for navigation-event navigation-args)]
    {:href routable-path
     :on-click
     (fn [e]
       (.preventDefault e)
       ;; We're about to give control over to the browser; when we get control
       ;; back, we'll need some info about where we've come from
       (handle-message events/stash-nav-stack-item nav-stack-item)
       (history/enqueue-navigate navigation-event navigation-args))}
    (fake-href navigation-event navigation-args)))

(defn route-to-shop [navigation-event & [args]]
  {:href
   (str (uri/map->URI {:host (str/replace-first js/location.host #"^[^\.]*\." "shop.")
                       :path (routes/path-for navigation-event args)}))})

(defn route-back [{:keys [navigation-message]}]
  {:href (apply routes/path-for navigation-message)
   :on-click
   (fn [e]
     (.preventDefault e)
     ;; use history.back(), so that events/browser-navigate is triggered
     (js/history.back))})

(defn route-back-or-to [back navigation-event & [navigation-args]]
  (if back
    (route-back back)
    (route-to navigation-event navigation-args)))

(defn requesting?
  "Look in app-state to see if we are waiting on a request to a particular endpoint. This returns falsey before the request gets made, so it's only useful if you also check to see whether the desired data is fetched."
  ([data request-key] (requesting? data :request-key request-key))
  ([data request-search request-key]
   (query/get
    {request-search request-key}
    (get-in data keypaths/api-requests))))

(defn requesting-from-endpoint?
  "Determines if a request to a particular endpoint is in flight regardless of
  additional args. e.g. If there are requests to [:search-v2-product criteria-a]
  and [:search-v2-product criteria-b] either will match
  (requesting-from-endpoint? data [:search-v2-product])"
  [data request-key]
  (some
   #(= request-key (-> % :request-key first vector))
   (get-in data keypaths/api-requests)))

(defn suppress-return-key [e]
  (when (= 13 (.-keyCode e))
    (.preventDefault e)))

(defn stop-propagation [e]
  (.stopPropagation e)
  false)

(defn scroll-href [anchor-id]
  {:href (str "#" anchor-id)
   :on-click (fn [e]
               (scroll/scroll-selector-to-top (str "a[name='" anchor-id "']"))
               (.preventDefault e))})

(defn toggle-checkbox [keypath value]
  (let [checked-val (if value "checked" "")]
    {:checked checked-val
     :value checked-val
     :on-change
     (fn [e]
       (handle-message events/control-change-state
                       {:keypath keypath
                        :value (.. e -target -checked)}))}))

(defn change-file [event]
  {:on-change (fn [e]
                (handle-message event
                                {:file (-> (.. e -target -files)
                                           array-seq
                                           first)}))})

(defn select-all-text [e]
  (let [el (.-target e)
        length (.-length (.-value el))]
    (cond
      (.-createTextRange el)
      (doto (.createTextRange el)
        (.collapse true)
        (.moveStart "character" 0)
        (.moveEnd "character" length)
        (.select))

      (.-setSelectionRange el)
      (.setSelectionRange el 0 length)

      :else
      (do
        (set! (.-selectionStart el) 0)
        (set! (.-selectionEnd el) length)))))

