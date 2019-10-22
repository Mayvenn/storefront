(ns storefront.component
  (:require [clojure.string :as str]
            [clojure.set :as set]
            #?@(:cljs [[cljsjs.react]
                       [goog]
                       [om.core]
                       [goog.object :as gobj]
                       [sablono.core :as sablono :refer-macros [html]]
                       ["react" :as react]
                       [storefront.platform.component-utils :as utils]]
                :clj [[storefront.safe-hiccup :as safe-hiccup]]))
  #?(:cljs (:require-macros [storefront.component :refer [defcomponent]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Port om-like component functions

#?(:cljs (defn get-props [component-instance]
           ;; NOTE: using gobj/get for react properies, and .- for our own
           ;; TODO(jeff): EXTERNS
           (some-> component-instance (gobj/get "props") .-props))
   :clj (defn get-props [component-instance]))

#?(:cljs (defn get-opts [component-instance]
           ;; NOTE: using gobj/get for react properies, and .- for our own
           ;; TODO(jeff): EXTERNS
           (some-> component-instance (gobj/get "props") .-options))
   :clj (defn get-opts [component-instance]))


#?(:cljs (defn set-state! [component-instance & key-values]
           ;; NOTE: using gobj/get for react properies, and .- for our own
           ;; TODO(jeff): EXTERNS
           (.setState component-instance (fn [s] #js {:state (apply assoc (.-state s) key-values)})))
   :clj (defn set-state! [component-instance key value]))

#?(:cljs (defn get-state [component-instance]
           ;; NOTE: using gobj/get for react properies, and .- for our own
           ;; TODO(jeff): EXTERNS
           (-> component-instance (gobj/get "state") .-state))
   :clj (defn get-state [component-instance]))

#?(:cljs (defn create-ref! [component-instance name]
           (gobj/set component-instance (str "_ref_" name) (react/createRef)))
   :clj (defn create-ref! [component-instance key value]))

#?(:cljs (defn use-ref [component-instance name]
           (-> component-instance
                   (gobj/get (str "_ref_" name))))
   :clj (defn use-ref [component-instance key value]))

#?(:cljs (defn get-ref [component-instance name]
           (some-> (use-ref component-instance name)
                   (gobj/get "current")))
   :clj (defn get-ref [component-instance key value]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

#?(:clj
   (defmacro if-cljs
     "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
     [then else]
     (if (cljs-env? &env) then else)))

(defn map->styles [m]
  (str/join (map (fn [[k v]] (str (name k) ":" v ";")) m)))

(defn normalize-style [{:keys [style] :as attrs}]
  (if style
    (update-in attrs [:style] map->styles)
    attrs))

(defn remove-handlers [m]
  (into {} (remove (fn [[k _]] (.startsWith (name k) "on-")) m)))

(defn normalize-attrs [attrs]
  (-> attrs
      (set/rename-keys {:item-prop  :itemprop
                        :item-scope :itemscope
                        :item-type  :itemtype
                        :src-set    :srcset})
      (dissoc :dangerouslySetInnerHTML :key :data-test)
      remove-handlers
      normalize-style))

(declare normalize-elements)

#?(:clj (defn normalize-element [[tag & content]]
          (let [[attrs body] (if (map? (first content))
                               [(first content) (apply normalize-elements (next content))]
                               [nil (apply normalize-elements content)])]
            (cond
              (:dangerouslySetInnerHTML attrs) [tag (normalize-attrs attrs) (safe-hiccup/raw (-> attrs :dangerouslySetInnerHTML :__html))]
              attrs `[~tag ~(normalize-attrs attrs) ~@body]
              :else `[~tag ~@body])))
   :cljs (def normalize-element identity))

(defn ^:private element? [v]
  (and (vector? v) (keyword? (first v))))

(defn normalize-elements [& content]
  (for [expr content]
    (cond
      (element? expr) (normalize-element expr)
      (sequential? expr) (apply normalize-elements expr)
      :else expr)))

(defn ^:private has-fn?
  ([f] (has-fn? (atom nil) [] f))
  ([problems path f]
   (cond (map? f)
         (not-empty
          (remove false?
                  (for [[k v] f]
                    (if (fn? v)
                      (let [path' (conj (vec path) k)]
                        (swap! problems conj path')
                        (has-fn? problems path' v))
                      false))))
         (sequential? f)
         (not-empty (remove false? (map-indexed #(has-fn? problems (conj (vec path) %1) %2) f)))

         :else (do
                 (println "EVAL: " f)
                 (fn? f)))))

(defn build* [component data opts debug-data]
  #?(:clj (component data nil (:opts opts))
     :cljs (do
             ;; in dev mode: assert proper usage
             (when ^boolean goog/DEBUG
               (assert (not (has-fn? data))
                       (str "building " (:file debug-data) ":" (:line debug-data) " includes a function which is not recommended because it reforces constant rerenders")))
             (cond
               (gobj/get component "isNewStyleComponent" false)
               (react/createElement component
                                    #js{:props               data
                                        :options             (:opts opts)
                                        :key                 (:key opts)
                                        :isNewStyleComponent true})

               (gobj/get component "displayName" false) ;; secondary legacy
               (react/createElement component
                                    (let [props #js{:props data}]
                                      (when-let [k (:key opts)]
                                        (gobj/set props "key" (str k "@" (:file debug-data) ":" (:line debug-data))))
                                      props))

               :else
               (react/createElement (component data nil (:opts opts))
                                    (let [props #js{:props data}]
                                      (when-let [k (:key opts)]
                                        (gobj/set props "key" (str k "@" (:file debug-data) ":" (:line debug-data))))
                                      props))))))

(defn should-update [this next-props next-state]
  #?(:cljs
     (let [props         (get-props this)
           state         (get-state this)
           next-children (. next-props -children)
           next-props    (.props next-props)]
       (or (not= props next-props)
           (and state
                (not= state next-state))
           (not= (.. this -props -children)
                 next-children)))))

(defn create* [name f]
  #?(:clj (f)
     :cljs (utils/create-component
            nil
            {"displayName" name}
            {"render" (fn render [this] (f))
             "shouldComponentUpdate" should-update})))

(defn create-dynamic* [name ctor methods]
  #?(:clj [:div {:data-type "dynamic"}]
     :cljs (do
             ;; in dev mode: assert proper usage
             (when ^boolean goog/DEBUG
               (assert (methods "render")
                       (str "render method missing for " name (pr-str (keys methods)))))
             (utils/create-component
              ctor
              {"displayName"         name
               "isNewStyleComponent" true}
              methods))))

(defmacro html [content]
  `(if-cljs
     (sablono.core/html ~content)
     ~content))

(defmacro create
  "Creates a React.PureComponent (on the client) / simple html rendering (on the server)

  Note: the newer version is the 3-arity version

  Optionally can be provided a class name which can be useful for debugging in React."
  ([body]
   `(create* ~(str *ns* "/" (str "UnnamedComponent@" (:line (meta &form))))
             (fn [] (or (html ~body)
                        (html [:div.component-empty])))))
  ([class-name body]
   `(create* ~(str *ns* "/" (name class-name)) (fn [] (or (html ~body)
                                                          (html [:div.component-empty]))))))

(defmacro ^{:style/indent [1 :defn]} create-dynamic
  "Creates a React.Component (on the client) / simple html rendering (on the server)"
  [component-name & methods]
  (let [m               (into {} (map (juxt first #(conj % 'fn))) methods)
        allowed-methods {'did-mount     "componentDidMount"
                         'should-update "shouldComponentUpdate"
                         'render        "render"
                         'did-update    "componentDidUpdate"
                         'will-unmount  "componentWillUnmount"
                         'did-catch     "componentDidCatch"}]
    `(create-dynamic* ~(str *ns* "/" (or (name component-name) (str "UnnamedDynamicComponent@" (:line (meta &form)))))
                      ~(m 'constructor)
                      ~(select-keys (set/rename-keys m allowed-methods)
                                    (vals allowed-methods)))))

(defmacro build
  ([component] (let [metadata (meta &form)
                     key (name (gensym))]
                 `(build* ~component nil {:key ~key} ~metadata)))
  ([component data] (let [metadata (meta &form)
                          key (name (gensym))]
                      `(build* ~component ~data {:key ~key} ~metadata)))
  ([component data opts] (let [metadata (meta &form)
                               key (name (gensym))]
                           `(build* ~component ~data (merge {:key ~key} ~opts) ~metadata))))

(def create-pure-component #?(:cljs utils/create-pure-component))

#?(:clj
   (defn- defcomponent-cljs [component-name meta docstring body-fn]
     `(def ~component-name
        ~@docstring
        (create-pure-component
         nil
         {"displayName"         ~(str *ns* "/" (or (name component-name)
                                                   (str "UnnamedComponent@" (:line meta))))
          "isNewStyleComponent" true}
         {"render" (fn render# [this#]
                     (~body-fn (get-props this#) this# (get-opts this#)))}))))
#?(:clj
   (defn- defcomponent-clj [component-name meta docstring body-fn]
     `(defn ~component-name ~@docstring [data# owner# opts#]
        (~body-fn data# owner# opts#))))


#?(:clj
   (defmacro ^{:style/indent :defn} defcomponent
     "Wrapper for the common pattern of:

      (defn my-component [data owner opts]
        (component/create \"my-component\"
          [:div ...]))
      "
     ([component-name args body] `(defcomponent ~component-name nil ~args ~body))
     ([component-name docstring? args body]
      (let [docstring?     (when (string? docstring?) [docstring?])
            body-fn        (gensym)]
        `(let [~body-fn (fn ~args (html ~body))]
           ~(if (cljs-env? &env)
              (defcomponent-cljs component-name (meta &form) docstring? body-fn)
              (defcomponent-clj component-name (meta &form) docstring? body-fn)))
        ))
     #_
     (if (string? docstring?)
       `(def ~component-name ~docstring? (create ~(str *ns* "/" (name component-name)) ~@body))
       (let [body (conj body args)
             args docstring?]
         `(defn ~name ~args (create ~name ~@body))))))

(defmacro ^{:style/indent :defn} defdynamic-component [name args & methods]
  `(def ~name (create-dynamic ~name ~@methods)))
