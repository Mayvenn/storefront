var gulp = require('gulp');
var sass = require('gulp-sass');
var minifyCSS = require('gulp-minify-css');

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
