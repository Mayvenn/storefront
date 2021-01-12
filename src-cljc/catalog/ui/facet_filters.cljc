(ns catalog.ui.facet-filters
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.header :as header]
            [spice.selector :as selector]))


(defn summary-status-molecule
  [{:filtering-summary.status/keys [primary secondary]}]
  [:div.flex.justify-between
   [:div.bold.shout.content-4 primary]
   [:div.content-3 secondary]])

(c/defcomponent summary-pill-molecule
  [{:filtering-summary.pill/keys [primary primary-icon id target action-icon]}
   _
   {:keys [id]}]
  [:div.pb1
   {:key id}
   (ui/button-pill
    (cond-> {:class     "p1 mr1 black content-3"
             :data-test id}
      (not-empty target)
      (assoc :on-click
             (apply utils/send-event-callback target)))
    [:div.flex.items-center.px1
     [:span.mr1
      (when primary-icon
        ^:inline (svg/funnel {:class  "mrp3"
                              :height "9px"
                              :width  "10px"}))
      primary]
     (when action-icon
       ^:inline (svg/close-x
                 {:class  "stroke-white fill-gray"
                  :width  "13px"
                  :height "13px"}))])])

(c/defcomponent summary-organism
  [data _ _]
  (when (seq data)
    [:div.bg-white.py2.px3
     (summary-status-molecule data)
     [:div.flex.flex-wrap.py1
      (component/elements summary-pill-molecule data
                          :filtering-summary/pills)]]))

(defn header-reset-molecule
  [{:header.reset/keys [primary id target]}]
  (component/html
   [:div (ui/button-medium-underline-black
          (merge {:data-test id}
                 (apply utils/fake-href target))
          primary)]))

(defn header-done-molecule
  [{:header.done/keys [primary id target]}]
  (component/html
   [:div (ui/button-medium-underline-primary
          (merge {:data-test id}
                 (apply utils/fake-href target))
          primary)]))

(c/defcomponent section-filter-molecule
  [{:facet-filtering.section.filter/keys [primary target value icon-url]} _ {:keys [id]}]
  [:a.col-12.mb2.flex
   {:on-click (apply utils/send-event-callback target)
    :key      id}
   [:div (ui/check-box {:value     value
                        :id        id
                        :data-test id})]
   (when icon-url
     [:img.block.pr2
      {:style {:width  "50px"
               :height "30px"}
       :src   icon-url}])
   [:div
    primary]])

(defn section-title-molecule
  [{:facet-filtering.section.title/keys [primary target id rotated?]}]
  [:a.block.flex.justify-between.inherit-color.items-center
   (cond-> {:data-test id}
     target
     (merge (apply utils/fake-href target)))
   [:div.shout.title-2.proxima
    primary]
   [:div.flex.items-center
    (when rotated?
      {:class "rotate-180 mrp2"})
    ^:inline (svg/dropdown-arrow
              {:class  "fill-black"
               :height "20px"
               :width  "20px"})]])

(c/defcomponent section-organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.bg-white.px5.myp1
   {:key id}
   [:div.pyj1
    (section-title-molecule data)]
   (component/elements section-filter-molecule
                       data
                       :facet-filtering.section/filters)])

(c/defcomponent header-organism
  [data _ _]
  (header/mobile-nav-header
   {:class "border-bottom border-gray"}
   (header-reset-molecule data)
   (component/html
    [:div.center.proxima.content-1 "Filters"])
   (header-done-molecule data)))

(c/defcomponent panel-template
  [{:keys [header sections]} _ _]
  [:div.col-12.bg-white {:style {:min-height "100vh"}}
   (component/build header-organism
                    header)
   [:div.mynp1.bg-refresh-gray
    (component/elements section-organism
                        sections
                        :facet-filtering/sections)]])

(def ^:private select
  (comp seq (partial selector/match-all {:selector/strict? true})))

(defn summary<-
  "Takes (Biz)
   - Defined Facets
   - Subject of filter
   - User's desired Filters

   Produces (Viz)
   - Filtering Summary
     - Status (with Filtered Count)
     - Pills"
  [facets-db
   faceted-models
   {:facet-filtering/keys [panel-toggle-event
                           filters
                           filter-toggle-event
                           item-label]}]
  (let [filtering-options (->> filters
                               (mapcat (fn selections->options
                                         [[facet-slug option-slugs]]
                                         (let [facet-options (-> facets-db
                                                                 (get facet-slug)
                                                                 :facet/options)]
                                           (->> option-slugs
                                                (map #(get facet-options %))
                                                (map #(assoc % :facet/slug facet-slug))))))
                               concat)
        pills             (concat
                           [{:filtering-summary.pill/primary-icon :funnel
                             :filtering-summary.pill/primary      (if (empty? filtering-options)
                                                                    "Filters"
                                                                    (str "- " (count filtering-options)))
                             :filtering-summary.pill/target       [panel-toggle-event {:toggled? true}]}]
                           (mapv (fn options->pills
                                   [option]
                                   {:filtering-summary.pill/primary     (:option/name option)
                                    :filtering-summary.pill/target      [filter-toggle-event
                                                                         {:facet-key  (:facet/slug option)
                                                                          :option-key (:option/slug option)
                                                                          :toggled?   false}]
                                    :filtering-summary.pill/action-icon :close-x})
                                 filtering-options))]
    {:filtering-summary.status/primary   "Filter By:"
     :filtering-summary.status/secondary (ui/pluralize-with-amount
                                          (count (select filters faceted-models))
                                          item-label)
     :filtering-summary/pills            pills}))

(defn sections<-
  "Takes (Biz)
   - Defined Facets
   - User's desired Filters

   Produces (Viz)
   - Sections
   - Filters"
  [facets-db
   represented-facets ;; {:facet/slug (Set :option/slug)}
   {:facet-filtering/keys [section-toggle-event
                           sections
                           filters
                           filter-toggle-event]}]
  (->> (vals (select-keys facets-db (keys represented-facets)))
       (sort-by :filter/order)
       (map
        (fn facet->section [{facet-slug    :facet/slug
                             facet-name    :facet/name
                             facet-options :facet/options}]
          (let [section-toggled?    (contains? sections facet-slug)
                represented-options (get represented-facets facet-slug)]
            (cond-> {:id                             facet-slug
                     :facet-filtering.section.title/primary  (str "Hair " facet-name) ;; TODO: this might not always need to be prefixed with hair (category page possibly)
                     :facet-filtering.section.title/target   [section-toggle-event {:facet-key facet-slug :toggled? (not section-toggled?)}]
                     :facet-filtering.section.title/id       (str "section-filter-" facet-slug)
                     :facet-filtering.section.title/rotated? section-toggled?}
              section-toggled?
              (assoc :facet-filtering.section/filters
                     (->> (vals facet-options)
                          (sort-by :filter/order)
                          (keep
                           (fn option->filter [{option-slug   :option/slug
                                                option-name   :option/name
                                                option-swatch :option/rectangle-swatch}]
                             (when (contains? represented-options option-slug)
                               (let [filter-toggled? (contains?
                                                      (get filters facet-slug)
                                                      option-slug)]
                                 (cond->
                                     #:facet-filtering.section.filter
                                     {:primary option-name
                                      :target  [filter-toggle-event
                                                {:facet-key  facet-slug
                                                 :option-key option-slug
                                                 :toggled?   (not filter-toggled?)}]
                                      :value   filter-toggled?
                                      :url     option-name}
                                   option-swatch
                                   (assoc :facet-filtering.section.filter/icon-url
                                          (str "https://ucarecdn.com/"
                                               (ui/ucare-img-id option-swatch)
                                               "/-/format/auto/-/resize/50x/")))))))
                          vec))))))))
