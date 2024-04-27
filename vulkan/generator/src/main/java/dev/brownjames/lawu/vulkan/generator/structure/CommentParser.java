/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import dev.brownjames.lawu.vulkan.generator.*;

/**
 * An object responsible for parsing the top-comments of structures for their members
 */
final class CommentParser {
	private static final Pattern SNIPPET_PATTERN = Pattern.compile("(?s)(?<=\\{@snippet :).*(?=})");

	static final class ParseException extends Exception {
		private ParseException(String message) {
			super(message);
		}

		private final class Unchecked extends RuntimeException {
			public Unchecked() {
				super(ParseException.this);
			}

			public ParseException getCause() {
				return (ParseException) super.getCause();
			}
		}

		private Unchecked unchecked() {
			return new Unchecked();
		}
	}

	sealed interface ParsedDeclaration permits ParsedStructOrUnion, ParsedTypeDefinition {
		CharSequence name();
	}

	sealed interface Declarator permits Value, Pointer, Array, Function {
		Declarator function();

		Declarator pointer();

		Declarator array();
	}

	record Value() implements Declarator {
		@Override
		public Declarator function() {
			return new Function(this);
		}

		@Override
		public Declarator pointer() {
			return new Pointer(this);
		}

		@Override
		public Declarator array() {
			return new Array(this);
		}
	}

	record Pointer(Declarator to) implements Declarator {
		@Override
		public Declarator function() {
			return new Pointer(to.function());
		}

		@Override
		public Declarator pointer() {
			return new Pointer(to.pointer());
		}

		@Override
		public Declarator array() {
			return new Pointer(to.array());
		}
	}

	record Array(Declarator of) implements Declarator {
		@Override
		public Declarator function() {
			return new Array(of.function());
		}

		@Override
		public Declarator pointer() {
			return new Array(of.pointer());
		}

		@Override
		public Declarator array() {
			return new Array(of.array());
		}
	}

	record Function(Declarator returning) implements Declarator {
		@Override
		public Declarator function() {
			return new Function(returning.function());
		}

		@Override
		public Declarator pointer() {
			return new Function(returning.pointer());
		}

		@Override
		public Declarator array() {
			return new Function(returning.array());
		}
	}

	private record StructDeclaratorInformation(Declarator declarator, CharSequence name) {
		StructMember makeMember(CharSequence type) throws ParseException {
			return new StructMember(declarator, name, type);
		}

		static StructDeclaratorInformation named(CharSequence name) {
			return new StructDeclaratorInformation(new Value(), name);
		}

		StructDeclaratorInformation pointer() {
			return new StructDeclaratorInformation(declarator.pointer(), name);
		}

		StructDeclaratorInformation array() {
			return new StructDeclaratorInformation(declarator.array(), name);
		}

		StructDeclaratorInformation function() {
			return new StructDeclaratorInformation(declarator.function(), name);
		}
	}

	private record StructMemberInformation(Optional<CharSequence> type, Collection<StructDeclaratorInformation> members) {
		static StructMemberInformation empty() {
			return new StructMemberInformation(Optional.empty(), Collections.emptyList());
		}

		static StructMemberInformation type(CharSequence type) {
			return new StructMemberInformation(Optional.of(type), Collections.emptyList());
		}

		static StructMemberInformation member(StructDeclaratorInformation declarator) {
			return new StructMemberInformation(Optional.empty(), List.of(declarator));
		}

		StructMemberInformation aggregate(StructMemberInformation other) throws ParseException {
			if (type.isPresent() && other.type.isPresent()) {
				throw new ParseException("Multiple type-names found");
			}
			var type = this.type.or(other::type);

			var members = new ArrayList<>(this.members);
			members.addAll(other.members);

			return new StructMemberInformation(type, members);
		}

		Stream<StructMember> makeMembers() throws ParseException {
			var type = this.type.orElseThrow(() -> new ParseException("No type information found"));
			return members.stream().map(m -> {
				try {
					return m.makeMember(type);
				} catch (ParseException e) {
					throw e.unchecked();
				}
			});
		}
	}

	record StructMember(Declarator declarator, CharSequence name, CharSequence type) { }

	private record DeclarationInformation(
			boolean isTypeDefinition,
			Optional<CharSequence> typeDefinitionName,
			Optional<CharSequence> structureName,
			Collection<StructMember> structMembers
	) {
		static DeclarationInformation empty() {
			return new DeclarationInformation(false, Optional.empty(), Optional.empty(), Collections.emptyList());
		}

		static DeclarationInformation typeDefinitionSpecifier() {
			return new DeclarationInformation(true, Optional.empty(), Optional.empty(), Collections.emptyList());
		}

		static DeclarationInformation typeDefinitionName(CharSequence name) {
			return new DeclarationInformation(false, Optional.of(name), Optional.empty(), Collections.emptyList());
		}

		static DeclarationInformation structName(CharSequence name) {
			return new DeclarationInformation(false, Optional.empty(), Optional.of(name), Collections.emptyList());
		}

		static DeclarationInformation structMembers(Collection<StructMember> members) {
			return new DeclarationInformation(false, Optional.empty(), Optional.empty(), members);
		}

		DeclarationInformation aggregate(DeclarationInformation other) throws ParseException {
			if (isTypeDefinition && other.isTypeDefinition) {
				throw new ParseException("Multiple type-definition specifiers found");
			}
			var isTypeDefinition = this.isTypeDefinition || other.isTypeDefinition;

			if (typeDefinitionName.isPresent() && other.typeDefinitionName.isPresent()) {
				throw new ParseException("Multiple type-definition names found");
			}
			var typeDefinitionName = this.typeDefinitionName.or(other::typeDefinitionName);

			if (structureName.isPresent() && other.structureName.isPresent()) {
				throw new ParseException("Multiple structure names found");
			}
			var structureName = this.structureName.or(other::structureName);

			var structMembers = new ArrayList<>(this.structMembers);
			structMembers.addAll(other.structMembers);

			return new DeclarationInformation(isTypeDefinition, typeDefinitionName, structureName, structMembers);
		}

		private Optional<ParsedDeclaration> makeDeclaration() throws ParseException {
			if (isTypeDefinition) {
				return Optional.of(new ParsedTypeDefinition(
						typeDefinitionName.orElseThrow(() -> new ParseException("Missing type-definition name")),
						structureName.orElseThrow(() -> new ParseException("Missing alias name"))));
			}

			if (typeDefinitionName.isPresent()) {
				throw new ParseException("Unexpected type-definition name");
			}

			return structureName
					.map(name -> new ParsedStructOrUnion(name, structMembers));
		}
	}

	record ParsedStructOrUnion(CharSequence name, Collection<StructMember> members) implements ParsedDeclaration { }
	record ParsedTypeDefinition(CharSequence name, CharSequence alias) implements ParsedDeclaration { }

	private static final CVisitor<StructDeclaratorInformation> DECLARATOR_VISITOR = new CBaseVisitor<>() {
		@Override
		public StructDeclaratorInformation visitDeclarator(CParser.DeclaratorContext ctx) {
			var result = visitDirectDeclarator(ctx.directDeclarator());

			if (ctx.pointer() != null) {
				for (var _ : ctx.pointer().Star()) {
					result = result.pointer();
				}
			}

			return result;
		}

		@Override
		public StructDeclaratorInformation visitDirectDeclarator(CParser.DirectDeclaratorContext ctx) {
			if (ctx.Identifier() != null) {
				return StructDeclaratorInformation.named(ctx.Identifier().getText());
			}

			if (ctx.declarator() != null) {
				return visitDeclarator(ctx.declarator());
			}

			var result = visitDirectDeclarator(ctx.directDeclarator());

			if (ctx.LeftBracket() != null) {
				return result.array();
			}

			if (ctx.LeftParen() != null) {
				return result.function();
			}

			return result;
		}
	};

	private static final CVisitor<StructMemberInformation> STRUCT_MEMBER_VISITOR = new CBaseVisitor<>() {
		@Override
		public StructMemberInformation visitTypeSpecifier(CParser.TypeSpecifierContext ctx) {
			return StructMemberInformation.type(ctx.getText());
		}

		@Override
		public StructMemberInformation visitDeclarator(CParser.DeclaratorContext ctx) {
			return StructMemberInformation.member(ctx.accept(DECLARATOR_VISITOR));
		}

		@Override
		protected StructMemberInformation defaultResult() {
			return StructMemberInformation.empty();
		}

		@Override
		protected StructMemberInformation aggregateResult(StructMemberInformation aggregate, StructMemberInformation nextResult) {
			try {
				return aggregate.aggregate(nextResult);
			} catch (ParseException e) {
				throw e.unchecked();
			}
		}
	};

	private static final CVisitor<DeclarationInformation> DECLARATION_VISITOR = new CBaseVisitor<>() {
		@Override
		public DeclarationInformation visitStorageClassSpecifier(CParser.StorageClassSpecifierContext ctx) {
			if (ctx.Typedef() != null) {
				return DeclarationInformation.typeDefinitionSpecifier();
			}

			return defaultResult();
		}

		@Override
		public DeclarationInformation visitTypeSpecifier(CParser.TypeSpecifierContext ctx) {
			if (ctx.typedefName() != null) {
				return DeclarationInformation.typeDefinitionName(ctx.typedefName().getText());
			}

			if (ctx.structOrUnionSpecifier() != null) {
				var members = super.visitStructOrUnionSpecifier(ctx.structOrUnionSpecifier());

				if (ctx.structOrUnionSpecifier().Identifier() != null) {
					return aggregateResult(members,
							DeclarationInformation.structName(ctx.structOrUnionSpecifier().Identifier().getText()));
				} else {
					return members;
				}
			}

			return defaultResult();
		}

		@Override
		public DeclarationInformation visitStructDeclaration(CParser.StructDeclarationContext ctx) {
			try {
				return DeclarationInformation.structMembers(ctx.accept(STRUCT_MEMBER_VISITOR).makeMembers().toList());
			} catch (ParseException e) {
				throw e.unchecked();
			}
		}

		@Override
		protected DeclarationInformation defaultResult() {
			return DeclarationInformation.empty();
		}

		@Override
		protected DeclarationInformation aggregateResult(DeclarationInformation aggregate, DeclarationInformation nextResult) {
			try {
				return aggregate.aggregate(nextResult);
			} catch (ParseException e) {
				throw e.unchecked();
			}
		}
	};

	private static final CVisitor<Collection<ParsedDeclaration>> COMPILATION_UNIT_VISITOR = new CBaseVisitor<>() {
		@Override
		public Collection<ParsedDeclaration> visitDeclarationSpecifiers(CParser.DeclarationSpecifiersContext ctx) {
			try {
				return ctx.accept(DECLARATION_VISITOR)
						.makeDeclaration()
						.map(List::of)
						.orElse(List.of());
			} catch (ParseException e) {
				throw e.unchecked();
			}
		}

		@Override
		protected Collection<ParsedDeclaration> defaultResult() {
			return Collections.emptyList();
		}

		@Override
		protected Collection<ParsedDeclaration> aggregateResult(Collection<ParsedDeclaration> aggregate, Collection<ParsedDeclaration> nextResult) {
			var result = new ArrayList<>(aggregate);
			result.addAll(nextResult);
			return result;
		}
	};

	static Stream<ParsedDeclaration> parse(CharSequence comment) throws ParseException {
		var snippets = extractSnippets(comment);
		var parsers = snippets.map(snippet ->
				new CParser(new CommonTokenStream(new CLexer(CharStreams.fromString(snippet.toString())))));

		try {
			return parsers.flatMap(parser ->
					parser.compilationUnit().accept(COMPILATION_UNIT_VISITOR).stream());
		} catch (ParseException.Unchecked pe) {
			throw pe.getCause();
		}
	}

	/**
	 * Extracts any snippets found in the comment supplied
	 * @param comment the comment to parse
	 * @return a stream of snippets
	 */
	public static Stream<CharSequence> extractSnippets(CharSequence comment) {
		return SNIPPET_PATTERN.matcher(comment).results().map(MatchResult::group);
	}
}
