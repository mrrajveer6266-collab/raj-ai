const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

const PROJECT = path.resolve(process.env.HOME, "raj-ai");

function run(cmd, args = []) {
  try {
    return execFileSync(cmd, args, {
      cwd: PROJECT,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"]
    });
  } catch (e) {
    return (e.stdout || "") + (e.stderr || "");
  }
}

function findJsErrors() {
  const files = [];

  function walk(dir) {
    if (!fs.existsSync(dir)) return;

    for (const name of fs.readdirSync(dir)) {
      if (
        name === "node_modules" ||
        name === ".git" ||
        name === "backups"
      ) continue;

      const full = path.join(dir, name);
      const stat = fs.statSync(full);

      if (stat.isDirectory()) {
        walk(full);
      } else if (name.endsWith(".js")) {
        files.push(full);
      }
    }
  }

  walk(path.join(PROJECT, "core"));
  walk(path.join(PROJECT, "tools"));

  if (fs.existsSync(path.join(PROJECT, "server.js"))) {
    files.push(path.join(PROJECT, "server.js"));
  }

  const errors = [];

  for (const file of files) {
    const result = run("node", ["--check", file]);

    if (result.trim()) {
      errors.push({
        file: path.relative(PROJECT, file),
        error: result.trim()
      });
    }
  }

  return errors;
}

function findAndroidErrors() {
  const sourceDir = path.join(
    PROJECT,
    "android/app/src/main/java"
  );

  if (!fs.existsSync(sourceDir)) return "";

  const classes = path.join(
    PROJECT,
    "android/manual-build/classes"
  );

  fs.rmSync(classes, {
    recursive: true,
    force: true
  });

  fs.mkdirSync(classes, {
    recursive: true
  });

  const files = [];

  function walk(dir) {
    for (const name of fs.readdirSync(dir)) {
      const full = path.join(dir, name);
      const stat = fs.statSync(full);

      if (stat.isDirectory()) {
        walk(full);
      } else if (
        name.endsWith(".java") &&
        !name.startsWith("MainActivity_backup_")
      ) {
        files.push(full);
      }
    }
  }

  walk(sourceDir);

  try {
    execFileSync(
      "javac",
      [
        "-encoding",
        "UTF-8",
        "-source",
        "8",
        "-target",
        "8",
        "-cp",
        path.join(
          process.env.HOME,
          "android-sdk/platforms/android-35/android.jar"
        ),
        "-d",
        classes,
        ...files
      ],
      {
        cwd: PROJECT,
        encoding: "utf8",
        stdio: ["ignore", "pipe", "pipe"]
      }
    );

    return "";
  } catch (e) {
    return (
      (e.stdout || "") +
      (e.stderr || "")
    ).trim();
  }
}

const jsErrors = findJsErrors();
const androidError = findAndroidErrors();

console.log(
  JSON.stringify(
    {
      ok: jsErrors.length === 0 && !androidError,
      jsErrors,
      androidError
    },
    null,
    2
  )
);
