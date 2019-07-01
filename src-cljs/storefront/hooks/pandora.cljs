(ns storefront.hooks.pandora
  (:require [storefront.browser.tags :refer [insert-image-with-src]]))

(def sign-up-tracking-pixel
  "https://data.adxcel-ec2.com/pixel/?ad_log=referer&action=signup&pixid=46c7e26f-7d65-490c-b7ec-58d8dd4b2786")

(defn track-signup []
  (insert-image-with-src sign-up-tracking-pixel))
