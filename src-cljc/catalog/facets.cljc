(ns catalog.facets
  "Facets are possible shopping attributes like color, texture, length, etc."
  (:require [spice.maps :as maps]
            [storefront.keypaths :as keypaths]
            [clojure.set :as set]
            [clojure.string :as string]))

;; TODO spec out what a facet looks like

(def query-param>slug
  {:grade            :hair/grade
   :family           :hair/family
   :origin           :hair/origin
   :weight           :hair/weight
   :texture          :hair/texture
   :base-material    :hair/base-material
   :color            :hair/color
   :color-shorthand  :hair/color.shorthand
   :length           :hair/length
   :color.process    :hair/color.process
   :style            :wig/trait
   :lace-size        :hair.closure/area
   :face-shape       :tags/face-shape
   :fashion          :tags/style
   :event            :tags/event})

(def slug>query-param
  (set/map-invert query-param>slug))

(defn by-slug
  [data]
  (->> (get-in data keypaths/v2-facets)
       (map #(update % :facet/options (partial maps/index-by :option/slug)))
       (maps/index-by :facet/slug)))

(defn color-order-map
  [facets]
  (->> facets
       (filter #(= (:facet/slug %) :hair/color))
       first
       :facet/options
       (sort-by :filter/order)
       (map :option/slug)
       (map-indexed (fn [idx slug] [slug idx]))
       (into {})))

(defn get-color
  [color-slug facets]
  (->> facets
       (filter (fn [facet] (= (:facet/slug facet) :hair/color)))
       first
       :facet/options
       (filter (fn [color] (= (:option/slug color) color-slug)))
       first))

(defn hacky-fix-of-bad-slugs-on-facets [slug]
  (string/replace (str slug) #"#" ""))

(def color-shorthand
  {:facet/name    "Hair Color"
   :facet/slug    :hair/color.shorthand
   :filter/order  6 ; Same as color, which it replaces
   :facet/options (->> [{:title     "Blacks"
                         :slug      "black"
                         :image-id  "cf2e6d44-4e93-4792-801b-1e2aacdac408"
                         :selectors #{"#1-jet-black"
                                      "black"
                                      "hl-1b-blonde"
                                      "1b-soft-black"
                                      "s01a"
                                      "s02"
                                      "s02a"}}
                        {:title     "Browns"
                         :slug      "brown"
                         :image-id  "9830b374-9ad5-46f7-b9ed-842dc0770cee"
                         :selectors #{"#2-chocolate-brown"
                                      "1c-mocha-brown"
                                      "#4-caramel-brown"
                                      "6-hazelnut-brown"
                                      "b-s04-s06"
                                      "b-s04-s07"
                                      "b-s05-s06"
                                      "h-s04-s08"
                                      "b-s05w-s06"
                                      "b-s05-s07"
                                      "h-s06-s08"
                                      "s04"
                                      "s05"
                                      "s06w"}}
                        {:title     "Burgundys"
                         :slug      "burgundy"
                         :image-id  "5eadafa5-87ac-42ab-9af7-9be9621a0de9"
                         :selectors #{"vibrant-burgundy"}}
                        {:title     "Oranges"
                         :slug      "orange"
                         :image-id  "a038fa62-dac5-4a71-94b1-d1392962881f"
                         :selectors #{"tt-1b-orange"}}
                        {:title     "Blondes"
                         :slug      "blonde"
                         :image-id  "4fd70f0a-96ce-4b6d-bc23-674fc8d37aa3"
                         :selectors #{"dark-blonde"
                                      "ash-blonde-dark-roots"
                                      "blonde"
                                      "dark-blonde-dark-roots"
                                      "blonde-dark-roots"
                                      "18-chestnut-blonde"
                                      "60-golden-ash-blonde"
                                      "613-bleach-blonde"
                                      "b-s07w-s08"
                                      "b-s08-s10"
                                      "h-s07a-s10"
                                      "s08"
                                      "s09"
                                      "s09w"}}
                        {:title     "Reds"
                         :slug      "red"
                         :image-id  "27df119d-bc0a-4f97-8f97-ef31bf5694b9"
                         :selectors #{"s07rb"}}]
                       (map-indexed (fn [idx {:keys [title slug image-id selectors]}]
                                      (let [swatch-url (str "//ucarecdn.com/" image-id "/-/format/auto/" slug ".jpg")]
                                        [slug
                                         {:filter/order            idx
                                          :option/name             title
                                          :option/slug             slug
                                          :sku/name                title
                                          ;; TODO(jjh): Should these really all be the same? Do we need them all?
                                          :option/circle-swatch    swatch-url
                                          :option/image            swatch-url
                                          :option/rectangle-swatch swatch-url
                                          ;; TODO(jjh): What to do with these selectors?
                                          :selectors               selectors}])))
                       (into {}))})

(def color-slug>shorthand-slug
  (->> color-shorthand
       :facet/options
       (mapcat (fn [[slug {:keys [selectors]}]]
              (map (fn [selector] [selector slug]) selectors)))
       (into {})))

(def shorthand-slug>color-slugs
  (reduce (fn [acc [color-slug shorthand-slug]]
            (if (contains? acc shorthand-slug)
              (update acc shorthand-slug conj color-slug)
              (assoc acc shorthand-slug [color-slug])))
          {}
          color-slug>shorthand-slug))

(defn expand-shorthand-colors
  [{:as args
    color-shorthand :hair/color.shorthand}]
  (cond-> args
    color-shorthand (dissoc :hair/color.shorthand)
    color-shorthand (assoc :hair/color
                           (set (mapcat shorthand-slug>color-slugs color-shorthand)))))
