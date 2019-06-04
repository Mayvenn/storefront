var gulp = require('gulp');
var RevAll = require('gulp-rev-all');
var argv = require('yargs').argv;
var gzip = require('gulp-gzip');
var jsonTransform = require('gulp-json-transform');
var merge = require('merge-stream');
var gulpIgnore = require('gulp-ignore');
var debug = require('gulp-debug');
var runSequence = require('run-sequence');
var shell = require('gulp-shell');
var del = require('del');
var postcss = require("gulp-postcss");
var uglify = require('gulp-uglify');
var fs = require('fs');
var path = require('path');
var os = require('os');
var exec = require('child_process').exec;

if (process.versions.node.search('12.') === -1) {
	console.error("Hey, you need to upgrade node to 12.x.x!");
	console.error("");
	console.error("This means running the following:");
	console.error("  brew upgrade node");
	console.error("  rm -rf node_modules");
	console.error("  npm install");
	console.error("");
	console.error("If you installed gulp globally (aka - you never run node_modules/gulp/bin/gulp.js directly), then you need to reinstall that too:");
	console.error("  npm uninstall -g gulp");
	console.error("  npm install -g gulp");
	process.exit(1);
}

exports.css = css;
function css() {
  return gulp.src(['./resources/css/*.css'])
    .pipe(postcss([
      require('postcss-import')(),
      require('postcss-custom-media')(),
      require('postcss-custom-properties')(),
      require('postcss-calc')(),
      require('postcss-color-function')(),
      require('postcss-discard-comments')(),
      require('autoprefixer')({browsers: ['last 3 versions']}),
      /* require('postcss-reporter')(), */
      /* comment out cssnano to see uncompressed css */
      require('cssnano')()
    ]))
    .pipe(gulp.dest('./resources/public/css'));
}

exports.watch = gulp.series(css, watch);
function watch(cb) { // depends on css
  gulp.watch(['./resources/css/*.css'], css);
}

exports.default = css;

/* Run this after you update node module versions. */
/* Maybe there's a preferred way of including node modules in cljs projects? */
exports['refresh-deps'] = shell.task([
	"cp",
	"./node_modules/react-slick/dist/react-slick.js",
	"./node_modules/jsqr/dist/jsQR.js",
	"src-cljs/storefront/"].join(" "));

exports['clean-min-js'] = cleanMinJs;
function cleanMinJs() {
  return del(['./target/min-js']);
};

exports['minify-js'] = gulp.series(cleanMinJs, minifyJs);
function minifyJs() {
  return gulp.src('src-cljs/storefront/*.js')
    .pipe(uglify({mangle: {reserved: ["jsQR"]}}))
    .pipe(gulp.dest('target/min-js/'));
}

exports['move-jsqr'] = shell.task(['mkdir -p ./resources/public/js/out/src-cljs/storefront/; mv target/min-js/jsQR.js resources/public/js/out/src-cljs/storefront/jsQR.js']);
exports['cljs-build'] = shell.task(['lein cljsbuild once release']);

exports['copy-release-assets'] = copyReleaseAssets;
function copyReleaseAssets(){
  return gulp.src(['./target/release/**'])
    .pipe(gulp.dest('./resources/public/'));
}

exports['clean-hashed-assets'] = cleanHashedAssets;
function cleanHashedAssets() {
  return del(['./resources/public/cdn', './resources/rev-manifest.json']);
};

exports['fix-source-map'] = fixSourceMap;
function fixSourceMap() {
  return sourceMapStream = gulp.src(['resources/public/js/out/main.js.map'], {base: './'})
    .pipe(jsonTransform(function(data) {
      data["sources"] = data["sources"].map(function(f) {
        return f.replace("\/", "/");
      });
      return data;
    }))
    .pipe(gulp.dest('./'));
}

exports['save-git-sha-version'] = saveGitShaVersion;
function saveGitShaVersion(cb) {
  exec('git show --pretty=format:%H -q', function (err, stdout) {
    if (err) {
      cb(err);
    } else {
      fs.writeFile('resources/client_version.txt', stdout, function (err) {
        if (err) return cb(err);
        return cb();
      });
    }
  });
}

function hashedAssetSources () {
  return merge(gulp.src('resources/public/{js,css,images,fonts}/**')
               .pipe(gulpIgnore.exclude("*.map")),
               gulp.src('resources/public/js/out/main.js.map'));
}

exports['rev-assets'] = revAssets;
function revAssets() {
  if (!argv.host) {
    throw "missing --host";
  }

  var revAll = new RevAll({
    prefix: "//" + argv.host + "/cdn/",
    dontSearchFile: ['[^jsQR].js']
  });

  return hashedAssetSources()
    .pipe(revAll.revision())
    .pipe(gulp.dest('resources/public/cdn'))
    .pipe(revAll.manifestFile())
    .pipe(gulp.dest('resources'));
};

exports['fix-main-js-pointing-to-source-map'] = fixMainJsPointingToSourceMap;
function fixMainJsPointingToSourceMap(cb) {
  // because .js files are excluded from search and replace of sha-ed versions (so that
  // the js code doesn't become really wrong), we need to take special care to update
  // main.js to have the sha-ed version of the sourcemap in the file
  fs.readFile("resources/rev-manifest.json", 'utf8', function(err, data) {
    if (err) { cb(err); return console.log(err); }

    var revManifest = JSON.parse(data),
        mainJsFilePath = "resources/public/cdn/" + revManifest["js/out/main.js"];

    fs.readFile(mainJsFilePath, 'utf8', function (err,data) {
      if (err) { return console.log(err); }
      var result = data.replace(/main\.js\.map/g, path.basename(revManifest["js/out/main.js.map"]));

      fs.writeFile(mainJsFilePath, result, 'utf8', function (err) {
        if (err) { return console.log(err); }
        cb();
      });
    });
  });
}

exports['gzip'] = gzip;
function gzip(){
  return gulp.src('resources/public/cdn/**')
    .pipe(gzip({ append: false }))
    .pipe(gulp.dest('resources/public/cdn'));
};

exports['write-js-stats'] = writeJsStats;
function writeJsStats(cb) {
  fs.readFile('resources/rev-manifest.json', 'utf8', function(err, data) {
    if (err) { cb(err); return console.log(err); }

    var revManifest = JSON.parse(data),
        mainJsFilePath = "resources/public/cdn/" + revManifest["js/out/main.js"];

    exec('wc -c "' + mainJsFilePath + '" | awk \'{print $1}\'', function(err, stdout){
      if (err) {
        cb(err);
      } else {
        var fileSize = stdout.trim();
        var fileCommand = (os.platform() == "darwin") ? "zless" : "zcat";
        exec('(time -p ' + fileCommand + ' ' + mainJsFilePath + '| node --check 2>/dev/null 1>/dev/null) 2>&1 | head -n1 | awk \'{print $2}\'', {shell: '/bin/bash'}, function(err, stdout) {
          var parseTime = stdout.trim();

          fs.writeFile("resources/main.js.file_size.stat", fileSize, function(err) {
            if (err) {
              cb(err);
            } else {
              fs.writeFile("resources/main.js.parse_time.stat", parseTime, function(err) {
                if (err) {
                  cb(err);
                } else {
                  console.log("==== MAIN JS STATS ====");
                  console.log('File Size: ' + fileSize + ' bytes');
                  console.log('Relative Parse Time: ' + parseTime + ' seconds');
                  console.log("=======================");
                }
              });
            }
          });
        });
      }
    });
  });
};

exports['cdn'] = gulp.series(cleanHashedAssets, fixSourceMap, revAssets, fixMainJsPointingToSourceMap, gzip);

exports['compile-assets'] = gulp.series(css, minifyJs, exports['move-jsqr'], exports['cljs-build'], copyReleaseAssets, exports['cdn'], saveGitShaVersion, writeJsStats);
