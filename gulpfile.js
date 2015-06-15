var gulp = require('gulp');
var sass = require('gulp-sass');
var minifyCSS = require('gulp-minify-css');
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
