var gulp = require('gulp');
var sass = require('gulp-sass');
var minifyCSS = require('gulp-minify-css');
var gulpMerge = require('gulp-merge');
var replace = require('gulp-replace');
var RevAll = require('gulp-rev-all');
var path = require('path');
var argv = require('yargs').argv;
var rimraf = require('gulp-rimraf');

gulp.task('sass', function () {
  gulp.src('./resources/scss/*.scss')
    .pipe(sass({errLogToConsole: true,
                indentedSyntax: true,
                includePaths: require('node-bourbon').includePaths}))
    .pipe(minifyCSS())
    .pipe(gulp.dest('./resources/public/css'));
});

gulp.task('watch', function () {
  gulp.watch('./resources/scss/*.scss', ['sass']);
});

gulp.task('default', ['sass']);

gulp.task('copy-assets', function () {
  gulp.src('../diva/app/assets/fonts/**/*')
    .pipe(gulp.dest('resources/public/fonts'));

  gulp.src('../diva/app/assets/images/**/*.{png,jpg}')
    .pipe(gulp.dest('./resources/public/images'));
  var pipe1 = gulp.src('../diva/vendor/assets/stylesheets/spree/frontend/mayvenn/**/*.scss')
      .pipe(gulp.dest('./resources/scss'));
  var pipe2 = gulp.src('../diva/vendor/assets/styleshess/spree/frontend/all.css.scss')
      .pipe(gulp.dest('./resources/scss/all.scss'));
  var pipe3 = gulp.src('../diva/vendor/assets/styleshess/spree/frontend/_free_shipping_banner.scss')
      .pipe(gulp.dest('./resources/scss/_free_shipping_banner.scss'));

  gulpMerge(pipe1, pipe2, pipe3)
    .pipe(replace("font-url\('", "url('/fonts/"))
    .pipe(replace('image-url\("', 'url("/images/'))
    .pipe(replace("image-url\('", "url('/images/"))
    .pipe(replace("image-url\('", "url('/images/"))
    .pipe(replace("spree\/frontend\/mayvenn\/", ""))
    .pipe(replace("spree\/frontend\/", ""))
    .pipe(gulp.dest('./resources/scss'))
});

gulp.task('cdn', function () {
  if (!argv.host) {
    throw "missing --host";
  }

  var revAll = new RevAll({
    prefix: "//" + argv.host + "/cdn/"
  });

  gulp.src(['./resources/public/cdn', './resources/rev-manifest.json'],
           { read: false })
    .pipe(rimraf());

  gulp.src('./resources/public/{js,css,images,fonts}/**')
    .pipe(revAll.revision())
    .pipe(gulp.dest('./resources/public/cdn'))
    .pipe(revAll.manifestFile())
    .pipe(gulp.dest('./resources'));
});
