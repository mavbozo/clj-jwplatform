(defproject me.maverickbozo/clj-jwplatform "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [medley "0.7.0"]
                 [clj-time "0.11.0"]
                 [clj-http "2.0.0"]
                 [crypto-random "1.2.0"]
                 [pandect "0.5.4"]
                 [prismatic/schema "1.0.1"]
                 [camel-snake-kebab "0.3.2"]
                 [com.cemerick/url "0.1.1"]
                 [org.clojure/test.check "0.8.2"]])
