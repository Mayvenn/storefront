(ns homepage.ui.diishan
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]))

;; TODO why do we need to adjust the top?
(defn ^:private vertical-squiggle-atom
  [top]
  [:div.absolute.col-12.flex.justify-center
   {:style {:top top}}
   (svg/vertical-squiggle
    {:style {:height "72px"}})])

(def ^:private mayvenn-logo-atom
  [:div.flex.justify-center
   ^:inline (svg/mayvenn-logo {:width "52px" :height "30px"})])

(defn ^:private diishan-quote-molecule
  [{:diishan.quote/keys [text]} breakpoint]
  (let [quotation-mark (svg/quotation-mark
                        (if (= :mobile breakpoint)
                          {:width  "21px" :height "18px"}
                          {:width  "35px" :height "30px"}))]
    (if (= :mobile breakpoint)
      [:div.flex.mx5.my10
       [:div quotation-mark]
       [:div.canela.title-2.center.pt2.pb4 text]
       [:div.self-end.rotate-180 quotation-mark]]
      [:div.flex.justify-center
       [:div quotation-mark]
       [:div.canela.title-1.center.mt2.mb4.col-7-on-dt.col-9-on-tb text]
       [:div.self-end.rotate-180 quotation-mark]])))

(defn ^:private diishan-attribution-molecule
  [{:diishan.attribution/keys [primary secondary ucare-ids]} breakpoint]
  (let [diishan-portrait (ui/defer-ucare-img
                           {:class "block col-12"
                            :width (if (= :mobile breakpoint)
                                     "600"
                                     "1000")}
                           (get ucare-ids breakpoint))]
    (if (= :mobile breakpoint)
      [:div.relative
       [:div.absolute.white.right-0.py8.px4.right-align
        [:div.proxima.title-2.shout primary]
        [:div secondary]]
       diishan-portrait]
      [:div.relative.col-6
       [:div.absolute.white.right-0.py6.px4.right-align
        [:div.proxima.title-1.shout primary]
        [:div secondary]]
       diishan-portrait])))

(c/defcomponent organism
  [data _ _]
  [:div
   [:div.hide-on-mb.flex
    [:div.mx5.col-6.flex.items-center
     [:div
      mayvenn-logo-atom
      (diishan-quote-molecule data :desktop)]]
    (diishan-attribution-molecule data :desktop)]
   [:div.relative.hide-on-tb-dt
    (vertical-squiggle-atom "-86px")
    (diishan-quote-molecule data :mobile)
    (diishan-attribution-molecule data :mobile)]])
