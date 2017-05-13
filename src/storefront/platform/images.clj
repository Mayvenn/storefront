(ns storefront.platform.images)

(defn platform-hq-image [attrs]
  ;; Present, so hq-url is downloaded, but transparent, so we don't see it slowly load in.
  [:img.col-12.absolute.overlay.transparent attrs])


