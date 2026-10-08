const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

const PROJECT = path.resolve(process.env.HOME, "raj-ai");
const ALLOWED = [
  ".js",
  ".json",
  ".java"
];

function safeFile(file) {
  const full = path.resolve(PROJECT, file);

  if (
    full !== PROJECT &&
    !full.startsWith(PROJECT + path.sep)
  ) {
    throw new Error("Project के बाहर file access allowed नहीं है।");
  }

  const ext = path.extname(full);

  if (!ALLOWED.includes(ext)) {
    throw new Error("इस file type को AI repair में बदलने की अनुमति नहीं है।");
  }

  return full;
}

function askAI(prompt) {
  try {
    return execFileSync(
      "node",
      ["-e", `
const { askAI } = require("./core/ai");
askAI(process.argv[1]).then(x => {
  process.stdout.write(String(x || ""));
}).catch(e => {
  process.stderr.write(String(e.message || e));
  process.exit(1);
});
      `, prompt],
      {
        cwd: PROJECT,
        encoding: "utf8",
        timeout: 120000
      }
    ).trim();
  } catch (e) {
    throw new Error(
      (e.stderr || e.stdout || e.message || "AI repair failed").trim()
    );
  }
}

function extractJSON(text) {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");

  if (start === -1 || end === -1 || end <= start) {
    throw new Error("AI ने valid repair JSON नहीं दिया।");
  }

  return JSON.parse(
    text.slice(start, end + 1)
  );
}

async function main() {
  const report = JSON.parse(
    execFileSync(
      "node",
      ["tools/auto-fix/repair.js"],
      {
        cwd: PROJECT,
        encoding: "utf8"
      }
    )
  );

  if (report.ok) {
    console.log("NO_REPAIR_NEEDED");
    return;
  }

  const prompt = `
तुम RAJ AI के सुरक्षित coding repair assistant हो।

Project directory:
${PROJECT}

Diagnostic report:
${JSON.stringify(report, null, 2)}

Rules:
1. केवल diagnostic error को fix करो।
2. Project directory के बाहर की files को touch मत करो।
3. केवल .js, .json और .java files allowed हैं।
4. Dependencies बदलने की जरूरत हो तो repair मत करो।
5. Secrets, API keys या .env को कभी मत पढ़ो या बदलो।
6. पूरा project rewrite मत करो।
7. छोटा और targeted patch दो।
8. अगर error पर्याप्त स्पष्ट नहीं है तो repair_needed=false रखो।

केवल यह JSON लौटाओ:
{
  "repair_needed": true,
  "file": "relative/path/to/file.js",
  "content": "पूरी नई file content"
}

अगर safe repair संभव नहीं है:
{
  "repair_needed": false,
  "reason": "कारण"
}
`;

  const raw = await askAI(prompt);
  const repair = extractJSON(raw);

  if (!repair.repair_needed) {
    console.log(
      JSON.stringify(
        {
          ok: false,
          repaired: false,
          reason: repair.reason || "Safe repair available नहीं है।"
        },
        null,
        2
      )
    );
    return;
  }

  if (!repair.file || typeof repair.content !== "string") {
    throw new Error("AI repair response incomplete है।");
  }

  const full = safeFile(repair.file);

  if (!fs.existsSync(full)) {
    throw new Error("AI ने ऐसी file चुनी जो मौजूद नहीं है।");
  }

  fs.writeFileSync(
    full,
    repair.content,
    "utf8"
  );

  console.log(
    JSON.stringify(
      {
        ok: true,
        repaired: true,
        file: repair.file
      },
      null,
      2
    )
  );
}

main().catch(error => {
  console.error(
    JSON.stringify(
      {
        ok: false,
        error: error.message
      },
      null,
      2
    )
  );

  process.exit(1);
});
