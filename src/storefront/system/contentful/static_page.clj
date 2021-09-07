(ns storefront.system.contentful.static-page
  (:require [storefront.system.contentful.graphql :as gql]
            [storefront.system.scheduler :as scheduler]
            [storefront.component :refer [normalize-element]]
            [hiccup.core :refer [html]]
            [spice.maps :as maps]
            [meander.epsilon :as m]
            [markdown.core :as markdown]
            [clojure.string :as string]
            [com.stuartsierra.component :refer [Lifecycle]]))

(defn- content-map [c]
  (apply maps/deep-merge
         (m/search c (m/and (m/$ {:sys {:id ?id} :as ?m})
                            (m/guard (not (:type (:sys ?m)))))
                   {?id ?m})))

(defn- content-html
  "Converts contentful linked structure into a hiccup-like format for page rendering"
  ([c exception-handler] (content-html c (content-map c) exception-handler))
  ([c id->entry exception-handler]
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

     ;; rich text field's structure
     {:nodeType "document" :content [(m/cata !content) ...]}                    (m/subst [:div.inline . !content ...])
     {:nodeType "heading-1" :content [(m/cata !content) ...]}                   (m/subst [:h1.title-1.canela.my4 . !content ...])
     {:nodeType "heading-2" :content [(m/cata !content) ...]}                   (m/subst [:h2.title-2.canela.my4 . !content ...])
     {:nodeType "heading-3" :content [(m/cata !content) ...]}                   (m/subst [:h3.title-3.canela.my4 . !content ...])
     {:nodeType "heading-4" :content [(m/cata !content) ...]}                   (m/subst [:h4.title-3.canela.my3 . !content ...])
     {:nodeType "heading-5" :content [(m/cata !content) ...]}                   (m/subst [:h5.title-3.canela.my2 . !content ...])
     {:nodeType "heading-6" :content [(m/cata !content) ...]}                   (m/subst [:h6.title-3.canela.my1 . !content ...])
     {:nodeType "blockquote" :content [(m/cata !content) ...]}                  (m/subst [:blockquote . !content ...])
     {:nodeType "paragraph" :content [(m/cata !content) ...]}                   (m/subst [:p.content-2.my2 . !content ...])
     {:nodeType "embedded-asset-block" :data {:target {:sys {:id ?id}}}}        (let [asset (id->entry ?id)]
                                                                                  [:img.block.col-12
                                                                                   {:style  {:height "auto"}
                                                                                    :src    (:url asset)
                                                                                    :alt    (str (:title asset))
                                                                                    :width  (:width asset)
                                                                                    :height (:height asset)}])
     {:nodeType "embedded-asset-inline" :data {:target {:sys {:id ?id}}}}        (let [asset (id->entry ?id)]
                                                                                  [:img
                                                                                   {:src    (:url asset)
                                                                                    :alt    (str (:title asset))
                                                                                    :width  (:width asset)
                                                                                    :height (:height asset)}])
     {:nodeType "embedded-entry-block" :data {:target {:sys {:id ?id}}}}        [:div.block (content-html (id->entry ?id) c)]
     {:nodeType "embedded-entry-inline" :data {:target {:sys {:id ?id}}}}       (content-html (id->entry ?id) c)
     {:nodeType "hyperlink" :content [(m/cata !content) ...] :data {:uri ?uri}} (m/subst [:a {:href ?uri} . !content ...])
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
     {:json (m/cata ?json)} ?json

     ;; error handling
     ?x (do
          (println "Missing clause in `content-html` for" (pr-str ?x))
          (exception-handler (IllegalArgumentException. (format "Missing clause in `content-html` for" (pr-str ?x))))
          [:div.bg-red.white "Unrecognized content type: " (pr-str ?x)]))))

(defn- fetch-raw [contentful path {:keys [preview? exception-handler]}]
  (let [pg (-> (gql/query contentful "static_page.gql" {"preview" (boolean preview?)
                                                        "path"    (str path)})
               :body
               :data
               :staticPageCollection
               :items
               first)]
    (when pg
      (:content pg))))

(defn- fetch [contentful path {:keys [preview? exception-handler]}]
  (let [pg (-> (gql/query contentful "static_page.gql" {"preview" (boolean preview?)
                                                        "path"    (str path)})
               :body
               :data
               :staticPageCollection
               :items
               first)]
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
      (into {}
            (map (fn [page] [(:path page) page]))
            pgs))))


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
                                                                                                  :exception-handler println}))))
  )
