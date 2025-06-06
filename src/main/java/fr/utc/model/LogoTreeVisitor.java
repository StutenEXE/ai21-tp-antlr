package fr.utc.model;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import fr.utc.gui.Traceur;
import fr.utc.parsing.LogoParser;
import fr.utc.parsing.LogoParser.*;
import javafx.beans.property.StringProperty;

public class LogoTreeVisitor extends LogoStoppableTreeVisitor {
	fr.utc.gui.Traceur traceur;
	Log log;
	private Deque<Integer> loopMemory; // Memoir LIFO de mes loops
	private Deque<Map<String, Double>> tableSymbole;
	// on utilise 2 liste pour autorisé la déclaration d'une fonction ave cle même nom que une procédure
	private Map<String, Pair<Liste_instructionsContext, Map<String, Double>>> procedure;
	private Map<String, Pair<Liste_instructionsContext, Map<String, Double>>> fonction;
	// Queue de retour des fonctions
	private Deque<Double> retour;
	
	public LogoTreeVisitor() {
		traceur = new Traceur();
		log = new Log();
		this.loopMemory = new ArrayDeque<Integer>();  // Memoire des loop LIFO
		this.tableSymbole = new ArrayDeque<Map<String,Double>>(); // table des symboles LIFO
		this.tableSymbole.push(new HashMap<String, Double>()); // table des symboles du bloc principal
		this.procedure = new HashMap<String, Pair<Liste_instructionsContext, Map<String, Double>>>();
		this.fonction = new HashMap<String, Pair<Liste_instructionsContext, Map<String, Double>>>();
		this.retour = new ArrayDeque<Double>();
	}

	public StringProperty logProperty() {
		return log;
	}

	public fr.utc.gui.Traceur getTraceur() {
		return traceur;
	}

	/*
	 * Map des attributs associés à chaque noeud de l'arbre
	 * key = node, value = valeur de l'expression du node
	 */
	ParseTreeProperty<Double> atts = new ParseTreeProperty<Double>();

	public void setValue(ParseTree node, double value) {
		atts.put(node, value);
	}

	public double getValue(ParseTree node) {
		Double value = atts.get(node);
		if (value == null) {
			throw new NullPointerException();
		}
		return value;
	}

	@Override
	public Integer visitBc(BcContext ctx){
		traceur.setCrayon(true);
		log.defaultLog(ctx);
		log.appendLog("Crayon baisser");
		return 0;
	}
	
	@Override
	public Integer visitFpos(FposContext ctx){
		Pair<Integer, List<Double>> bilan = evaluateList(ctx.expr());
		if(bilan.a == 0 && bilan.b.size()==2) {
			traceur.teleport(bilan.b.get(0), bilan.b.get(1));
			log.defaultLog(ctx);
			log.appendLog("Possition définit à : ", String.valueOf(bilan.b));
			
			return 0;
		}
		return 1;
	}
	
	@Override
	public Integer visitLc(LcContext ctx){
		traceur.setCrayon(false);
		log.defaultLog(ctx);
		log.appendLog("Crayon Lever");
		return 0;
	}

	@Override
	public Integer visitTd(TdContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a droite");
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitRe(ReContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(-bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Recule de ", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitTg(TgContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(-bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a gauche");
		}
		return bilan.a;
	}
	
	@Override 
	public Integer visitFcc(FccContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.setColor(bilan.b.intValue());
			log.defaultLog(ctx);
			log.appendLog("Couleur changer pour la n°", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitFcap(FcapContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			log.defaultLog(ctx);
			log.appendLog("Cap de la tortue fixé à : ", String.valueOf(bilan.b));
			traceur.fixeCap(bilan.b);
		}
		return bilan.a;
	}

	@Override
	public Integer visitAv(AvContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(bilan.b);
			// Différents type de log possibles. Voir classe Log
			log.defaultLog(ctx);
			log.appendLog("Avance de", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitStore(StoreContext ctx) {
		traceur.store();
		log.defaultLog(ctx);
		log.appendLog("Position sauvegardé");
		return 0;
	}
	
	@Override
	public Integer visitMove(MoveContext ctx) {
		log.defaultLog(ctx);
		if(!traceur.move()) {
			log.appendLog("Impossible de changer de position, aucune position sauvegardé");
		}else {
			log.appendLog("Changement pour l'ancienne position sauvegardé");	
		}
		return 0;
	}
	
	@Override
	public Integer visitAffectation(AffectationContext ctx) {
		Pair<Integer, Double> b = evaluate(ctx.expr());
		if (b.a==0) {
			log.appendLog("Symbol : ", ctx.NAME().getText(), " Définit à : ", String.valueOf(b.b));
			this.tableSymbole.peek().put(ctx.NAME().getText(), b.b);
		}
		return b.a;
	}
	
	@Override
	public Integer visitIf(IfContext ctx) {
		int comparaison = visit(ctx.predicat());
		if(comparaison == 0) {
			// Si vrai faire 1er instruction
			if(getValue(ctx.predicat()) == 1) {
				visit(ctx.liste_instructions(0));
			//SInon le 2
			}else {
				// Si il existe
				if(ctx.liste_instructions().size() == 2) {
					visit(ctx.liste_instructions(1));	
				}
			}
		}
		return comparaison;
	}
	
	@Override
	public Integer visitRepete(RepeteContext ctx) {
		int b = visit(ctx.expr());
		if(b==0) {
			// Table des symboles du block
			this.tableSymbole.push(new HashMap<String, Double>());
			for (int i=0; i<getValue(ctx.expr()); i++) {
				// Ajout dans la pile de la valeur de la boucles
				this.loopMemory.push(i+1);
				visit(ctx.liste_instructions());
				// Suppression de la valeur obscolète de la boucle
				this.loopMemory.pop();
			}	
			// On dépile les symbole
			this.tableSymbole.pop();
		}
		return b;
	}
	
	@Override
	public Integer visitTantque(TantqueContext ctx) {
		int comparaison = visit(ctx.predicat());
		if(comparaison == 0) {
			// On balance la boucle de tant que;
			this.tableSymbole.push(new HashMap<String, Double>());
			int i =1;
			while(getValue(ctx.predicat()) == 1) {
				this.loopMemory.push(i);
				visit(ctx.liste_instructions());
				this.loopMemory.pop();
				i++;
				visit(ctx.predicat());
			}
			this.tableSymbole.pop();
		}
		return comparaison;
	}
		
	@Override
	public Integer visitDeclarationProcedure(DeclarationProcedureContext ctx) {
		String procedureName = ctx.NAME().getText();
		Map<String, Double> symboles = new HashMap<String, Double>();
		for (TerminalNode s : ctx.VAR()) {
			symboles.put(s.getText(), null);
		}
		// Fonction ou Procédure ? 
		this.log.defaultLog(ctx);
		if(this.isFonction(ctx.liste_instructions())) {
			this.fonction.put(procedureName, new Pair<Liste_instructionsContext, Map<String, Double>>(ctx.liste_instructions(), symboles));
			this.log.appendLog("Définition de la fonction : ", procedureName);
		}else {
			this.procedure.put(procedureName, new Pair<Liste_instructionsContext, Map<String, Double>>(ctx.liste_instructions(), symboles));
			this.log.appendLog("Définition de la procédure : ", procedureName);
		}
		return 0;
	}
	
	@Override
	public Integer visitExecuteProcedure(ExecuteProcedureContext ctx) {					
		if(this.procedure.containsKey(ctx.NAME().getText())) {
			Pair<Liste_instructionsContext, Map<String, Double>> proc =  this.procedure.get(ctx.NAME().getText());
			if(ctx.expr().size() != proc.b.size()) {
				log.appendLog("Erreur, nombre de paramètre attendu : ", String.valueOf(proc.b.size()), " Mais reçu : ", String.valueOf(ctx.expr().size()));
				return 1;
			}else {
				int i=0;
				// Définition des variables de la table des symboles
				for(String keys : proc.b.keySet()) {
					int b=visit(ctx.expr(i));
					if(b==0) {
						proc.b.put(keys, getValue(ctx.expr(i)));	
					}else {
						return 1;
					}
					i++;
				}
				// Lancement de l'exécution:
				this.tableSymbole.push(proc.b);
				log.defaultLog(ctx);
				log.appendLog("Execution de la procedure ", ctx.NAME().getText());
				int b = visit(proc.a);
				this.tableSymbole.pop();
				return b;
			}
		}
		log.appendLog("Erreur, procedure inconnue : ", ctx.NAME().getText());
		return 1;
	}
	
	
	@Override
	public Integer visitRetourFonction(RetourFonctionContext ctx) {
		int b = visit(ctx.expr());
		if(b==0) {
			this.retour.push(getValue(ctx.expr()));
		}
		return b;
	}
	
	// Boolean
	
	@Override
	public Integer visitBooleanComparaison(BooleanComparaisonContext ctx) {
		Pair<Integer, Integer> comparaison = evaluateComparaison(ctx.expr(0), ctx.expr(1), ctx.getChild(1).getText());
		if(comparaison.a == 0) {
			log.defaultLog(ctx);
			setValue(ctx, comparaison.b);
		}
		return comparaison.a;
	}
	
	
	// Expressions

	@Override
	public Integer visitFloat(FloatContext ctx) {
		String floatText = ctx.FLOAT().getText();
		setValue(ctx, Double.valueOf(floatText));
		return 0;
	}
	
	@Override
	public Integer visitParenthese(ParentheseContext ctx) {
	    int b = visit(ctx.expr());
	    if (b == 0) {
	        Double val = getValue(ctx.expr());
	        setValue(ctx, val);
	    }
	    return b;
	}
	
	@Override
	public Integer visitSum(SumContext ctx) {
		Pair<Integer, Double> left, right;
		left = evaluate(ctx.expr(0));
		right = evaluate(ctx.expr(1));
		if (left.a == 0 && right.a == 0) {
			String sign = ctx.getChild(1).getText();
			Double r = sign.equals("+") ? left.b + right.b :
			left.b - right.b;
			setValue(ctx, r);
			return 0;
		}
		return left.a == 0 ? right.a : left.a;
	}
	
	@Override
	public Integer visitMult(MultContext ctx) {
		Pair<Integer, Double> left, right;
		left = evaluate(ctx.expr(0));
		right = evaluate(ctx.expr(1));
		if (left.a == 0 && right.a == 0) {
			String sign = ctx.getChild(1).getText();
			Double r = Double.POSITIVE_INFINITY;
			if(sign.equals("*")) {
				r = left.b * right.b;
			}else {
				if(right.b != 0) {
					r = left.b / right.b;
				}else {
					return 1;
				}
			}
			setValue(ctx, r);
			return 0;
		}
		return left.a == 0 ? right.a : left.a;
	}
	
	@Override
	public Integer visitHasard(HasardContext ctx) {
		int b = visit(ctx.expr());
	    if (b == 0) {
	        Double max = getValue(ctx.expr());
	        double result = Math.random() * max;
	        setValue(ctx, result);
	    }
	    return b;
	}
	
	@Override
	public Integer visitCos(CosContext ctx) {
		int b = visit(ctx.expr());
	    if (b == 0) {
	        Double value = getValue(ctx.expr());
	        double result = Math.cos(value);
	        setValue(ctx, result);
	    }
	    return b;
	}
	
	@Override
	public Integer visitSin(SinContext ctx) {
		int b = visit(ctx.expr());
	    if (b == 0) {
	        Double value = getValue(ctx.expr());
	        double result = Math.sin(value);
	        setValue(ctx, result);
	    }
	    return b;
	}
	
	@Override
	public Integer visitLoop(LoopContext ctx) {
		if(this.loopMemory.isEmpty()) {
			log.appendLog("Impossible d'utiliser loop sans repétition");
			return 1;
		}
		Integer value = this.loopMemory.element();
		setValue(ctx, value);
		return 0;
	}
	
	@Override
	public Integer visitVariables(VariablesContext ctx) {
		for (Map<String, Double> mapDesBlock : this.tableSymbole) {
			if (mapDesBlock.containsKey(ctx.VAR().getText())){
				setValue(ctx, mapDesBlock.get(ctx.VAR().getText()));
				return 0;
			}
		}
		log.appendLog("Symbol \"", ctx.VAR().getText(), "\" est introuvable");
		return 1;
	}
	
	@Override
	public Integer visitComparaison(ComparaisonContext ctx) {
		Pair<Integer, Integer> comparaison = evaluateComparaison(ctx.expr(0), ctx.expr(1), ctx.getChild(1).getText());
		if(comparaison.a == 0) {
			log.defaultLog(ctx);
			setValue(ctx, comparaison.b);
		}
		return comparaison.a;
	}
	
	@Override
	public Integer visitExecuteFonction(ExecuteFonctionContext ctx) {
		if(this.fonction.containsKey(ctx.NAME().getText())) {
			Pair<Liste_instructionsContext, Map<String, Double>> proc =  this.fonction.get(ctx.NAME().getText());
			if(ctx.expr().size() != proc.b.size()) {
				log.appendLog("Erreur, nombre de paramètre attendu : ", String.valueOf(proc.b.size()), " Mais reçu : ", String.valueOf(ctx.expr().size()));
				return 1;
			}else {
				int i=0;
				// Définition des variables de la table des symboles
				for(String keys : proc.b.keySet()) {
					int b=visit(ctx.expr(i));
					if(b==0) {
						proc.b.put(keys, getValue(ctx.expr(i)));	
					}else {
						return 1;
					}
					i++;
				}
				// Lancement de l'exécution:
				this.tableSymbole.push(proc.b);
				log.defaultLog(ctx);
				log.appendLog("Execution de la Fonction ", ctx.NAME().getText());
				int b = visit(proc.a);
				this.tableSymbole.pop();
				if(b==0) {
					double retour = this.retour.pop();
					setValue(ctx, retour);
				}
				return b;
			}
		}
		log.appendLog("Erreur, fonction inconnue : ", ctx.NAME().getText());
		return 1;
	}
	
	/**
	 * Visite le noeud expression
	 * S'il n'y a pas d'erreur (la valeur de retour de la visite vaut 0)
	 * on récupère la valeur de l'expressions à partir de la map
	 * sinon
	 * on affecte une valeur quelconque
	 * On retourne une paire, (code de visite, valeur)
	 * 
	 * @param expr
	 * @return
	 */
	private Pair<Integer, Double> evaluate(ParseTree expr) {
		int b = visit(expr);
		Double val = b == 0 ? getValue(expr) : Double.POSITIVE_INFINITY;
		return new Pair<Integer, Double>(b, val);
	}
	
	private Pair<Integer, List<Double>> evaluateList(List<ExprContext> expr) {
		List<Double> values = new ArrayList<>();
		int code = 0;
		for(ExprContext e : expr) {
			int b = visit(e);
			Double val = b == 0 ? getValue(e) : Double.POSITIVE_INFINITY;
			code +=b;
			values.add(val);	
		}
		return new Pair<>(code, values);
	}
	
	private Pair<Integer, Integer> evaluateComparaison(ExprContext left, ExprContext right, String sign){
		int b = visit(left);
		Double valL = b == 0 ? getValue(left) : Double.POSITIVE_INFINITY;
		b += visit(right);
		Double valR = b == 0 ? getValue(right) : Double.POSITIVE_INFINITY;
		if (b==0) {
			if(sign.equals("<")) {
				log.appendLog("Comparaison de  ", String.valueOf(valL), " < ", String.valueOf(valR));
				return new Pair<Integer, Integer>(b, (valL < valR ? 1: 0));
			}else {
				log.appendLog("Comparaison de  ", String.valueOf(valL), " > ", String.valueOf(valR));
				return new Pair<Integer, Integer>(b, (valL > valR ? 1: 0));
			}
		}
		return new Pair<Integer, Integer>(b, Integer.MAX_VALUE);
	}
	
	private boolean isFonction(Liste_instructionsContext liste) {
		for(InstructionContext list : liste.instruction()) {
			if(list instanceof RetourFonctionContext) {
				log.appendLog("FOnction");
				return true;
			}
		}
		return false;
	}

}
