var gulp = require('gulp');
var RevAll = require('gulp-rev-all');
var argv = require('yargs').argv;
var gzip = require('gulp-gzip');
var jsonTransform = require('gulp-json-transform');
var merge = require('merge-stream');
var gulpIgnore = require('gulp-ignore');
var debug = require('gulp-debug');
var runSequence = require('run-sequence');
var del = require('del');
var postcss = require("gulp-postcss");
var uglify = require('gulp-uglify');
var fs = require('fs');
var path = require('path');
var os = require('os');
var {exec} = require('child_process');

function run(cmd, cb) {
  var p = exec(cmd);
  p.stdout.on('data', function (data) {
    process.stdout.write(data.toString());
  });

  p.stderr.on('data', function (data) {
    process.stderr.write(data.toString());
  });

  p.on('exit', function (code) {
    cb(code === 0 ? null : ('child process exited with code ' + code.toString()));
  });
}

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
exports['refresh-deps'] = function refreshDeps(cb) {
  run([
    "cp",
    "./node_modules/react-slick/dist/react-slick.js",
    "./node_modules/jsqr/dist/jsQR.js",
    "src-cljs/storefront/"].join(" "), cb);
}

exports['clean-min-js'] = cleanMinJs;
function cleanMinJs() {
  return del(['./target/min-js']);
};

exports['minify-js'] = gulp.series(cleanMinJs, minifyJs);
function minifyJs() {
  return gulp.src('src-cljs/storefront/*.js')
    .pipe(uglify())
    .pipe(gulp.dest('target/min-js/'));
}

exports['cljs-build'] = function cljsBuild(cb) {
  run('lein cljsbuild once release', cb);
};

exports['copy-release-assets'] = copyReleaseAssets;
function copyReleaseAssets(){
  return gulp.src(['./target/release/**'])
    .pipe(gulp.dest('./resources/public/'));
}

exports['clean-hashed-assets'] = cleanHashedAssets;
function cleanHashedAssets() {
  return del(['./resources/public/cdn', './resources/rev-manifest.json']);
}

exports['fix-source-map'] = fixSourceMap;
function fixSourceMap() {
  return sourceMapStream = gulp.src(['resources/public/js/out/main.js.map',
                                     'resources/public/js/out/cljs_base.js.map',
                                     'resources/public/js/out/redeem.js.map'], {base: './'})
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
               .pipe(gulpIgnore.exclude("*.map")));
}

exports['rev-assets'] = revAssets;
function revAssets() {
  if (!argv.host) {
    throw "missing --host";
  }

  var options = {
    prefix: "//" + argv.host + "/cdn/",
    includeFilesInManifest: ['.css', '.js', '.svg', '.png', '.gif', '.woff', '.cljs', '.cljc', '.map'],
    dontSearchFile: ['.js']
  };

  return hashedAssetSources()
    .pipe(RevAll.revision(options))
    .pipe(gulp.dest('resources/public/cdn'))
    .pipe(RevAll.manifestFile())
    .pipe(gulp.dest('resources'));
}

exports['cp-source-maps'] = copySourceMaps;
function copySourceMaps() {
  return gulp.src('resources/public/js/out/*.map')
    .pipe(gulp.dest('resources/public/cdn/js/out'));
}

exports['fix-main-js-pointing-to-source-map'] = gulp.series(copySourceMaps, fixMainJsPointingToSourceMap);
function fixMainJsPointingToSourceMap(cb) {
  // because .js files are excluded from search and replace of sha-ed versions (so that
  // the js code doesn't become really wrong), we need to take special care to update
  // main.js to have the sha-ed version of the sourcemap in the file
  fs.readFile("resources/rev-manifest.json", 'utf8', function(err, data) {
    if (err) { cb(err); return console.log(err); }

    var revManifest = JSON.parse(data);

    let jsRootFiles = ["js/out/cljs_base.js", "js/out/main.js", "js/out/redeem.js"];
    let base = "resources/public/cdn/";

    var i = 0;
    jsRootFiles.forEach(function(jsKey){
      var originalFullJsMapFile = base + jsKey + ".map";
      var finalFullJsMapFile = base + revManifest[jsKey] + ".map";
      fs.renameSync(originalFullJsMapFile, finalFullJsMapFile);
      revManifest[jsKey + ".map"] = revManifest[jsKey] + ".map";

      var fullJsFile = "resources/public/cdn/" + revManifest[jsKey];
      fs.readFile(fullJsFile, 'utf8', function (err,data) {
        if (err) { return console.log(err); }
        function escapeRegExp(string) {
          return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string
        }
        var result = data.replace(new RegExp(escapeRegExp(path.basename(jsKey)), 'g'), path.basename(revManifest[jsKey]));

        fs.writeFile(fullJsFile, result, 'utf8', function (err) {
          if (err) { return console.log(err); }
          i++;

          if (jsRootFiles.length <= i) {
            fs.writeFile("resources/rev-manifest.json", JSON.stringify(revManifest, null, 2), 'utf8', cb);
          }
        });
      });
    });
  });
}

exports['gzip'] = function gzipTask(){
  return gulp.src('resources/public/cdn/**')
    .pipe(gzip({ append: false }))
    .pipe(gulp.dest('resources/public/cdn'));
};

exports['write-js-stats'] = writeJsStats;
function writeJsStats(cb) {
  fs.readFile('resources/rev-manifest.json', 'utf8', function(err, data) {
    if (err) { cb(err); return console.log(err); }

    let revManifest = JSON.parse(data),
        mainJsFilePath = "resources/public/cdn/" + revManifest["js/out/main.js"],
        cljsBaseFilePath = "resources/public/cdn/" + revManifest["js/out/cljs_base.js"];

    exec('wc -c "' + mainJsFilePath + '" "' + cljsBaseFilePath + '" | awk \'{print $1}\' | tail -n 1', function(err, stdout){
      if (err) {
        cb(err);
      } else {
        var fileSize = stdout.trim();
        exec('(time -p /bin/bash -c \'cat "' + cljsBaseFilePath  +  '" "' + mainJsFilePath + '" | gunzip -c | node --check\' 2>/dev/null 1>/dev/null) 2>&1 | head -n1 | awk \'{print $2}\'', {shell: '/bin/bash'}, function(err, stdout) {
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
                  cb();
                }
              });
            }
          });
        });
      }
    });
  });
}

exports['cdn'] = gulp.series(cleanHashedAssets, fixSourceMap, revAssets, exports['fix-main-js-pointing-to-source-map'], exports['gzip']);

exports['compile-assets'] = gulp.series(css, minifyJs, exports['cljs-build'], copyReleaseAssets, exports['cdn'], saveGitShaVersion, writeJsStats);
