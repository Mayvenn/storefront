(ns homepage.ui.quality-image
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [ui.molecules :refer [hero]]))

(c/defcomponent hero-image-component
  [{:screen/keys [seen?] :as data} owner opts]
  [:div (c/build hero (merge data {:off-screen? (not seen?)}) nil)])

(c/defcomponent molecule
  [data _ _]
  [:div.col-12.my5 {:key (str (:mob-uuid data))}
   (ui/screen-aware hero-image-component data nil)])
