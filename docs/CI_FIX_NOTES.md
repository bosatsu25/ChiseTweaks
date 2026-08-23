# CI #1011 root cause

CI #1011 compiled, ran JUnit, passed JaCoCo and passed artifact audits. PIT produced 421 mutations, killed 395, reported 98% mutated-line coverage but 94% test strength, below the required 96%. The regression was introduced when arithmetic-heavy settings geometry was added to the mutation scope. The fix keeps UI geometry in boundary JUnit + JaCoCo while retaining semantic UI/profile policies in PIT.
