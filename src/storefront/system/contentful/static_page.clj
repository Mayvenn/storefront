(ns storefront.system.contentful.static-page
  (:require [storefront.system.contentful.graphql :as gql]
            [storefront.system.scheduler :as scheduler]
            [storefront.component :refer [normalize-element]]
            [mayvenn.tracer :as tracer]
            [environ.core :refer [env]]
            [hiccup.core :refer [html]]
            [spice.maps :as maps]
            [meander.epsilon :as m]
            [markdown.core :as markdown]
            [clojure.string :as string]
            [com.stuartsierra.component :refer [Lifecycle]]))

(when-not (= (env :environment) "development")
  (tracer/instrument! markdown/md-to-html-string))

(defn- content-map
  "Takes a contentful tree of data and returns a map containing sys.id -> entry that exist.

  A bit more formally:
    content-map = map of ?id -> ?entry
    where ?entry = has a contentful id (a map containing the form {:sys {:id ?id}})
    and   ?entry != link to an entry (a map not containing {:sys {:type ?anything}})

  (Entry maps in Contentful do not contain types, so this search heuristic is used)

  This is useful to correlate any link / reference nodes to its corresponding entry."
  [c]
  (apply maps/deep-merge
         (m/search c (m/and (m/$ {:sys {:id ?id} :as ?m})
                            (m/guard (not (:type (:sys ?m)))))
                   {?id ?m})))
(when-not (= (env :environment) "development")
  (tracer/instrument! content-map))

(defn- content-html
  "Converts contentful linked structure into a hiccup-like format for page rendering"
  ([c exception-handler] (content-html c (content-map c) exception-handler))
  ([c id->entry exception-handler]
   ;; Meander crash course:
   ;;  1. Meader is a pattern-matching library with a cond-like form (match input condition1 output1 condition2 output2 ...)
   ;;  2. Rules for conditions (pattern matching)
   ;;    a. Symbols with ? prefixed are variables to match against (variable)
   ;;    b. Symbols with ! prefixed are variables that contain every match that has been made (memory variables)
   ;;    c. Values must match for the output to be chosen
   ;;    d. '...' indicates repeatedly match everything to the left of the elipsis. Typically paired with memory variables.
   ;;         Example: [!a ...] -> all elements are stored in !a
   ;;       Conceptually, think of this as repeating the previous forms as much as possible
   ;;    e. '.' indicates a partition for pattern matching rules
   ;;         Example: [:foo . !bar ...] -> !bar contains all elements after :foo
   ;;    d. 'cata' recurses the pattern match, the result is exchanged with the variable given
   ;;  3. Rules for outputting
   ;;    a. By default, the output is just code to run
   ;;    b. 'subst' allows pattern matching like behavior, but for outputting.
   ;;       - Basically, allows '.' and '...' to be utilized to splat values out
   ;;    c. Variables (? & ! prefixed symbols) can be used
   (m/match c
     ;; content types
     {:__typename "Markdown" :title ?title :content ?content}
     [:div
      (when (pos? (count ?title)) [:h1.h1.my4 (str ?title)])
      [:div.content-markdown {:dangerouslySetInnerHTML {:__html (markdown/md-to-html-string ?content)}}]]

     {:__typename "Html" :html ?html}
     [:div.content-html {:dangerouslySetInnerHTML {:__html ?html}}]

     {:__typename "Paragraph" :title ?title :textAlignment ?align :text (m/cata ?html)}
     [:div.content-richtext
      (when (pos? (count ?title)) [:h1.h1.my4 ?title])
      [:div {:class (case ?align
                      "left" ""
                      "center" "center"
                      "right" "right")}
       ?html]]

     {:__typename "Variable" :type "static" :value ?value}
     [:span.inline (str ?value)]

     ;; rich text field's structure
     {:nodeType "document" :content [(m/cata !content) ...]}                    (m/subst [:div.inline . !content ...])
     {:nodeType "heading-1" :content [(m/cata !content) ...]}                   (m/subst [:h1.title-1.canela.my4.center . !content ...])
     {:nodeType "heading-2" :content [(m/cata !content) ...]}                   (m/subst [:h2.title-2.canela.my4 . !content ...])
     {:nodeType "heading-3" :content [(m/cata !content) ...]}                   (m/subst [:h3.title-3.canela.my4 . !content ...])
     {:nodeType "heading-4" :content [(m/cata !content) ...]}                   (m/subst [:h4.title-3.canela.my3 . !content ...])
     {:nodeType "heading-5" :content [(m/cata !content) ...]}                   (m/subst [:h5.title-3.canela.my2 . !content ...])
     {:nodeType "heading-6" :content [(m/cata !content) ...]}                   (m/subst [:h6.title-3.canela.my1 . !content ...])
     {:nodeType "blockquote" :content [(m/cata !content) ...]}                  (m/subst [:blockquote . !content ...])
     {:nodeType "paragraph" :content [(m/cata !content) ...]}                   (m/subst [:p.content-2.my2 . !content ...])
     {:nodeType "embedded-asset-block" :data {:target {:sys {:id ?id}}}}        (let [asset (id->entry ?id)]
                                                                                  [:p.block.col-12
                                                                                   [:img.mx-auto.block
                                                                                    {:style  {:height "auto"}
                                                                                     :src    (:url asset)
                                                                                     :alt    (str (:title asset))
                                                                                     :width  (:width asset)
                                                                                     :height (:height asset)}]])
     {:nodeType "embedded-entry-block" :data {:target {:sys {:id ?id}}}}        [:div.block (content-html (id->entry ?id) c)]
     {:nodeType "embedded-entry-inline" :data {:target {:sys {:id ?id}}}}       (content-html (id->entry ?id) c)
     {:nodeType "hyperlink" :content [(m/cata !content) ...] :data {:uri ?uri}} (m/subst [:a.p-color.button-font-2.border-bottom.border-width-2.border-p-color
                                                                                          {:href ?uri} . !content ...])
     {:nodeType "hyperlink" :content [(m/cata !content) ...]}                   (m/subst [:a . !content ...])
     {:nodeType "unordered-list" :content [(m/cata !content) ...]}              (m/subst [:ul . !content ...])
     {:nodeType "ordered-list" :content [(m/cata !content) ...]}                (m/subst [:ol . !content ...])
     {:nodeType "list-item" :content [(m/cata !content) ...]}                   (m/subst [:li . !content ...])
     {:nodeType "hr"}                                                           [:hr]
     {:nodeType "text" :value ?text :marks []}                                  ?text
     {:nodeType "text" :value ?text :marks [{:type !marks} ...]}                (let [m (set !marks)]
                                                                                  [:span {:class (cond-> ""
                                                                                                   (m "bold") (str " bold")
                                                                                                   (m "italic") (str " italic")
                                                                                                   (m "underline") (str " underline"))}
                                                                                   ?text])
     {:json (m/cata ?json)}                                                     ?json

     ;; error handling
     ?x (do
          (println "Missing clause in `content-html` for" (pr-str ?x))
          (exception-handler (IllegalArgumentException. (format "Missing clause in `content-html` for" (pr-str ?x))))
          [:div.bg-red.white "Unrecognized content type: " (pr-str ?x)]))))
(when-not (= (env :environment) "development")
  (tracer/instrument! content-html))

(defn- fetch-raw [contentful path {:keys [preview?]}]
  (-> (gql/query contentful "static_page.gql" {"preview" (boolean preview?)
                                               "path"    (str path)})
      :body
      :data
      :staticPageCollection
      :items
      first))

(defn- fetch [contentful path {:keys [preview? exception-handler] :as options}]
  (when-let [pg (fetch-raw contentful path options)]
    (when pg
      {:title    (:title pg)
       :path     (:path pg)
       :contents [:div.container (content-html (:content pg) exception-handler)]})))


(defn- available-pages [contentful {:keys [preview?]}]
  (let [pgs (-> (gql/query contentful "all_static_pages.gql" {"preview" (boolean preview?)})
                :body
                :data
                :staticPageCollection
                :items
                not-empty)]
    (when pgs
      (into #{} (map :path) pgs))))


(defrecord Repository [contentful routes scheduler]
  Lifecycle
  (start [c]
    (let [options {:preview? false}
          routes  (atom nil)]
      (when-let [interval (:static-page-fetch-interval contentful)]
        (scheduler/every scheduler interval "contentful static page list"
                         #(when-let [pgs (available-pages contentful options)]
                            (reset! routes pgs))))
      (assoc c :routes routes)))
  (stop [c]
    (dissoc c :timer-pool)))

(defn has-page? [repo path]
  (when-let [r (:routes repo)] ;; if repo is nil, just assume nothing is available - probably dev err
    (let [r @r]
      (if (nil? r)
        true ;; if we haven't fetched data yet, just assume it's there
        (contains? r path)))))

(defn content-for
  "Returns the html string containing the page contents of the given static page"
  [repo path preview?]
  (let [preview? (boolean preview?)
        path (str path)
        path (if (string/starts-with? path "/static/") (.substring path (.length "/static")) path)]
    ;; preview pages are always assumed to exist, skip cache
    (when (or preview? (has-page? repo path))
      (when-let [pg (fetch (:contentful repo) path {:preview?          preview?
                                                    :exception-handler (:exception-handler repo)})]
        (html (normalize-element (:contents pg)))))))

(comment
  (:routes (:static-pages-repo dev-system/the-system))
  (has-page? (:static-pages-repo dev-system/the-system) "/policy/privacy")
  (content-for (:static-pages-repo dev-system/the-system) "/policy/privacy" false)

  (def r (gql/query (:contentful (:static-pages-repo dev-system/the-system))
                    "static_page.gql" {"preview" true
                                       "path"    "/policy/privacy"}))
  (content-map r)
  (def r (available-pages (:contentful dev-system/the-system) {"$preview" true}))

  (content-html {:json {:nodeType "document"
                        :content  [{:nodeType "text"
                                    :value    "hello"}]}})

  (content-html (-> r
                    :body
                    :data
                    :staticPageCollection
                    :items
                    first
                    :content))

  (fetch-raw (:contentful (:static-pages-repo dev-system/the-system)) "/policy/privacy" {:preview?          false
                                                                                         :exception-handler println})

  (storefront.safe-hiccup/html5
   (storefront.component/normalize-element
    (:contents (fetch (:contentful (:static-pages-repo dev-system/the-system)) "/policy/privacy" {:preview?          false
                                                                                                  :exception-handler println})))))
