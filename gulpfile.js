var gulp = require('gulp');
var sass = require('gulp-sass');
var minifyCSS = require('gulp-minify-css');
var RevAll = require('gulp-rev-all');
var path = require('path');
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
var concat = require('gulp-concat');

gulp.task('sass', function () {
  return gulp.src('./resources/scss/*.scss')
    .pipe(sass({errLogToConsole: true,
                indentedSyntax: true,
                includePaths: require('node-bourbon').includePaths}))
    .pipe(minifyCSS())
    .pipe(gulp.dest('./resources/public/css'));
});

gulp.task('css', function () {
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
});

gulp.task('unify-css', function () {
  return gulp.src(['./resources/public/css/all.css', './resources/public/css/app.css'])
    .pipe(concat('full.css'))
    .pipe(gulp.dest('./resources/public/css/'));
});

gulp.task('watch', function (cb) {
  runSequence(['sass', 'css'], 'unify-css', cb);
  gulp.watch('./resources/scss/*.scss', function () { runSequence('sass', 'unify-css')});
  gulp.watch('./resources/css/*.css', function () { runSequence('css', 'unify-css')});
});

gulp.task('default', function (cb) {
  runSequence(['sass', 'css'], 'unify-css', cb)
});

gulp.task('minify-js', function () {
  del(['./target/min-js']);

  gulp.src('src-cljs/storefront/*.js')
    .pipe(uglify())
    .pipe(gulp.dest('target/min-js/'));
});

gulp.task('cljs-build', shell.task(['lein cljsbuild once release']));

gulp.task('copy-release-assets', function () {
  return gulp.src(['./target/release/**'])
    .pipe(gulp.dest('./resources/public/'));
});

gulp.task('cdn', function () {
  if (!argv.host) {
    throw "missing --host";
  }

  if (!argv.sourcemaps_host) {
    throw "missing --sourcemaps_host";
  }

  // Clean up from last build
  del(['./resources/public/cdn', './resources/rev-manifest.json']);

  var revAll = new RevAll({
      prefix: "//" + argv.host + "/cdn/"
  });

  var sourceMapHost = "//" + argv.sourcemaps_host + "/sourcemaps";
  var sourceMapPath = 'resources/public/js/out/main.js.map';
  var sourceMapStream = gulp.src([sourceMapPath])
      .pipe(jsonTransform(function(data) {
        data["sources"] = data["sources"].map(function(f) {
          return sourceMapHost + f.replace("\/", "/");
        });
        return data;
      }));

  var fileStream = gulp.src(['resources/public/{css,images,fonts}/**', 'resources/public/js/out/main.js'])
      .pipe(gulpIgnore.exclude("*.map"));

  return merge(fileStream, sourceMapStream)
    .pipe(revAll.revision())
    .pipe(gzip({ append: false }))
    .pipe(gulp.dest('./resources/public/cdn'))
    .pipe(revAll.manifestFile())
    .pipe(gulp.dest('./resources'));
});

gulp.task('sourcemaps', function () {
  return gulp.src(['resources/public/js/**/*.{js,cljs,cljc,map}'])
      .pipe(gzip({ append: false }))
      .pipe(gulp.dest('./resources/public/sourcemaps/js/'));
});

gulp.task('compile-assets', function(cb) {
  runSequence(['sass', 'css'], 'unify-css', 'minify-js', 'cljs-build', 'copy-release-assets', 'cdn', 'sourcemaps', cb);
});
